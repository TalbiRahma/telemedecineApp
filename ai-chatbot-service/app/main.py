from fastapi import FastAPI
from pathlib import Path
import os, json
from dotenv import load_dotenv

from app.schemas import ChatRequest, ChatResponse
from app.rag_service import RagService

app = FastAPI(title="Prediagnostic Microservice")

service: RagService | None = None

load_dotenv()
SESSIONS_DIR = Path(os.environ.get("SESSIONS_DIR", "sessions"))

@app.on_event("startup")
def startup():
    global service
    service = RagService()

@app.get("/")
def root():
    return {"message": "API is running ✅"}

@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    assert service is not None
    return service.chat(req.session_id, req.message)

@app.get("/sessions/{session_id}")
def get_session(session_id: str):
    path = SESSIONS_DIR / f"{session_id}.json"
    if not path.exists():
        return {"session_id": session_id, "messages": []}
    return json.loads(path.read_text(encoding="utf-8"))

@app.delete("/sessions/{session_id}")
def delete_session(session_id: str):
    path = SESSIONS_DIR / f"{session_id}.json"
    if path.exists():
        path.unlink()
    return {"deleted": True, "session_id": session_id}

@app.get("/health")
def health():
    return {"status": "ok"}
