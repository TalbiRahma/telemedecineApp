import os
import json
from pathlib import Path
from dotenv import load_dotenv
from datetime import datetime, timezone

import re
from pydantic import ValidationError
from app.schemas import StructuredAnswer


from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS
from langchain_groq import ChatGroq
from langchain_core.prompts import ChatPromptTemplate


def extract_json(text: str) -> str:
    """Extract the first JSON object from an LLM response."""
    text = text.strip()
    decoder = json.JSONDecoder()
    for match in re.finditer(r"\{", text):
        candidate = text[match.start():]
        try:
            value, end = decoder.raw_decode(candidate)
        except json.JSONDecodeError:
            continue
        if isinstance(value, dict):
            return candidate[:end]

    return text


def normalize_structured_answer(data: dict) -> dict:
    """Apply safe, non-generative normalization before schema validation."""
    if not isinstance(data, dict):
        raise TypeError("The structured answer must be a JSON object")

    normalized = data.copy()
    follow_up_questions = normalized.get("follow_up_questions")
    if not isinstance(follow_up_questions, list):
        raise TypeError("follow_up_questions must be a list")

    normalized["follow_up_questions"] = follow_up_questions[:3]
    return normalized


def parse_structured_answer(raw: str) -> StructuredAnswer:
    """Extract, decode, normalize, and validate one LLM response."""
    data = json.loads(extract_json(raw))
    normalized = normalize_structured_answer(data)
    return StructuredAnswer.model_validate(normalized)


load_dotenv()

# --- RAG config ---
INDEX_DIR = Path(os.environ.get("INDEX_DIR", "faiss_index"))
EMBED_MODEL = os.environ.get("EMBED_MODEL", "sentence-transformers/all-mpnet-base-v2")

GROQ_MODEL = os.environ.get("GROQ_MODEL", "openai/gpt-oss-120b")
GROQ_TEMPERATURE = float(os.environ.get("GROQ_TEMPERATURE", "0.2"))

# --- Memory config ---
SESSIONS_DIR = Path(os.environ.get("SESSIONS_DIR", "sessions"))
RECENT_TURNS = int(os.environ.get("RECENT_TURNS", "2"))  # عدد turns (user+assistant) نخزنوهم
SUMMARY_MODEL = os.environ.get("SUMMARY_MODEL", "llama-3.1-8b-instant")
SUMMARY_MAX_CHARS = int(os.environ.get("SUMMARY_MAX_CHARS", "1200"))


def _safe_session_filename(session_id: str) -> str:
    keep = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
    cleaned = "".join(ch for ch in session_id if ch in keep)
    return cleaned or "default"

def _session_path(session_id: str) -> Path:
    SESSIONS_DIR.mkdir(parents=True, exist_ok=True)
    return SESSIONS_DIR / f"{_safe_session_filename(session_id)}.json"

def load_state(session_id: str) -> dict:
    path = _session_path(session_id)
    if not path.exists():
        return {"session_id": session_id, "summary": "", "recent_messages": []}
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return {
            "session_id": session_id,
            "summary": data.get("summary", ""),
            "recent_messages": data.get("recent_messages", []),
        }
    except Exception:
        return {"session_id": session_id, "summary": "", "recent_messages": []}

def save_state(session_id: str, summary: str, recent_messages: list) -> None:
    path = _session_path(session_id)
    payload = {
        "session_id": session_id,
        "updated_at": datetime.now(timezone.utc).isoformat(),
        "summary": summary,
        "recent_messages": recent_messages,
    }
    with open(path, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)

def recent_to_text(recent_messages: list) -> str:
    lines = []
    for m in recent_messages:
        role = m.get("role", "user")
        content = m.get("content", "")
        lines.append(f"{role}: {content}")
    return "\n".join(lines)

def trim_recent(recent_messages: list) -> list:
    # كل turn = زوج messages (user + assistant) عادة
    max_msgs = RECENT_TURNS * 2
    return recent_messages[-max_msgs:]


class RagService:
    def __init__(self):
        required_index_files = (INDEX_DIR / "index.faiss", INDEX_DIR / "index.pkl")
        missing_files = [path.name for path in required_index_files if not path.is_file()]
        if missing_files:
            raise RuntimeError(
                f"FAISS index is incomplete in '{INDEX_DIR}': missing {', '.join(missing_files)}"
            )
        if not os.environ.get("GROQ_API_KEY", "").strip():
            raise RuntimeError("GROQ_API_KEY must be configured")

        # Load FAISS
        embeddings = HuggingFaceEmbeddings(model_name=EMBED_MODEL)
        self.db = FAISS.load_local(
            str(INDEX_DIR),
            embeddings,
            allow_dangerous_deserialization=True
        )
        self.retriever = self.db.as_retriever(search_kwargs={"k": 4})

        # LLM for answering
        self.llm = ChatGroq(model=GROQ_MODEL, temperature=GROQ_TEMPERATURE)

        # LLM for summarizing (خفيف)
        self.summarizer = ChatGroq(model=SUMMARY_MODEL, temperature=0.1)

        # Answer prompt (uses summary + recent)
        self.answer_prompt = ChatPromptTemplate.from_messages([
    ("system",
     "You are a medical pre-diagnostic assistant.\n"
     "Use ONLY the provided context.\n"
     "Return ONLY valid JSON that matches this exact schema:\n"
     "{{\n"
     '  "chief_summary": "string",\n'
     '  "differential": [\n'
     '    {{"condition": "string", "likelihood": "LOW|MEDIUM|HIGH", "rationale": "string"}}\n'
     "  ],\n"
     '  "urgency": "LOW|MEDIUM|HIGH|EMERGENCY",\n'
     '  "urgency_reason": "string",\n'
     '  "next_steps": [\n'
     '    {{"category":"SELF_CARE|TESTS|MEDICATION_SAFETY|FOLLOW_UP", "text": "string"}}\n'
     "  ],\n"
     '  "red_flags": [{{"symptom": "string", "action": "string"}}],\n'
     '  "doctor": {{"specialty": "string", "when": "string"}},\n'
     '  "follow_up_questions": ["string", "string"],\n'
     '  "disclaimer": "string"\n'
     "}}\n\n"
     "Rules:\n"
     "- Output JSON ONLY. No markdown, no code fences.\n"
     "- follow_up_questions must contain exactly 2 or 3 useful questions. Never return more than 3.\n"
     "- Differential must be 3 to 5 items.\n"
     "- doctor.specialty must be a concise canonical medical specialty name in uppercase "
     "(for example CARDIOLOGY, DERMATOLOGY, GENERAL_MEDICINE), never a doctor name.\n"
     "- If EMERGENCY, add clear red_flags with action like 'Go to ER now'.\n"
     "- disclaimer must start with: 'DISCLAIMER:'\n"),
    ("human",
     "Context:\n{context}\n\n"
     "Conversation summary:\n{summary}\n\n"
     "Recent messages:\n{recent}\n\n"
     "User:\n{question}")
])


        # Summarization prompt: تحديث summary بطريقة متدرجة
        self.summary_prompt = ChatPromptTemplate.from_messages([
            ("system",
             "You update a running conversation summary for a medical pre-diagnostic chat.\n"
             "Keep it concise, factual, and clinically relevant.\n"
             "Include: symptoms, duration, severity, risk factors, meds/allergies (if mentioned), tests done, and any red flags.\n"
             "Do NOT include long explanations. Do NOT include personal identifiers.\n"
             f"Max length: {SUMMARY_MAX_CHARS} characters."),
            ("human",
             "Current summary:\n{summary}\n\n"
             "New exchange:\nUSER: {user}\nASSISTANT: {assistant}\n\n"
             "Return ONLY the updated summary text.")
        ])

    def update_summary(self, current_summary: str, user_msg: str, assistant_msg: str) -> str:
        prompt = self.summary_prompt.format(
            summary=current_summary,
            user=user_msg,
            assistant=assistant_msg
        )
        new_summary = self.summarizer.invoke(prompt).content.strip()
        # hard trim (safety)
        if len(new_summary) > SUMMARY_MAX_CHARS:
            new_summary = new_summary[:SUMMARY_MAX_CHARS]
        return new_summary

    def _parse_answer_with_one_repair(self, raw: str) -> StructuredAnswer:
        try:
            return parse_structured_answer(raw)
        except (json.JSONDecodeError, TypeError, ValidationError):
            repair_prompt = (
                "Fix the following content into valid JSON matching the requested medical "
                "answer schema. Return valid JSON only, with no markdown or code fences. "
                "follow_up_questions must be a JSON list containing exactly 2 or 3 useful "
                "questions; never return more than 3. Do not invent medical facts merely to "
                "satisfy the schema.\n\nCONTENT:\n" + raw
            )
            repaired = self.llm.invoke(repair_prompt).content
            try:
                return parse_structured_answer(repaired)
            except (json.JSONDecodeError, TypeError, ValidationError) as repair_error:
                raise ValueError(
                    "LLM response remained invalid after one repair attempt"
                ) from repair_error

    def chat(self, session_id: str, message: str) -> dict:
    # 1) Load state (summary + recent messages)
        state = load_state(session_id)
        summary = state["summary"]
        recent_messages = state["recent_messages"]

        # 2) Retrieval from FAISS
        docs = self.retriever.invoke(message)
        context = "\n\n---\n\n".join([d.page_content for d in docs])

        recent_text = recent_to_text(recent_messages)

        # 3) Build prompt for structured JSON answer
        formatted = self.answer_prompt.format(
            context=context,
            summary=summary,
            recent=recent_text,
            question=message
        )

        # 4) LLM call -> parse JSON -> validate with Pydantic
        raw = self.llm.invoke(formatted).content
        answer_obj = self._parse_answer_with_one_repair(raw)

        # 5) Update recent memory (keep it light)
        # We store a compact assistant note, not the whole JSON, to keep memory clean.
        top_dx = answer_obj.differential[0].condition if answer_obj.differential else ""
        assistant_note = f"urgency={answer_obj.urgency}; top_diagnosis={top_dx}"

        recent_messages.append({"role": "user", "content": message})
        recent_messages.append({"role": "assistant", "content": assistant_note})
        recent_messages = trim_recent(recent_messages)

        # 6) Update running summary
        summary = self.update_summary(summary, message, answer_obj.model_dump_json())

        # 7) Persist to disk
        save_state(session_id, summary, recent_messages)

        # 8) Return structured response
        return {
            "session_id": session_id,
            "answer": answer_obj.model_dump(),
            "summary": summary
        }


    

