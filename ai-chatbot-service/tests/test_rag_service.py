import json
import unittest
from types import SimpleNamespace

from app.rag_service import RagService, parse_structured_answer


def answer_payload(question_count: int = 2) -> dict:
    return {
        "chief_summary": "A brief symptom summary",
        "differential": [
            {
                "condition": "Condition one",
                "likelihood": "MEDIUM",
                "rationale": "A sufficiently detailed rationale",
            },
            {
                "condition": "Condition two",
                "likelihood": "LOW",
                "rationale": "Another sufficiently detailed rationale",
            },
        ],
        "urgency": "LOW",
        "urgency_reason": "No immediate red flags reported",
        "next_steps": [
            {"category": "SELF_CARE", "text": "Rest and monitor symptoms"},
            {"category": "FOLLOW_UP", "text": "Contact a clinician if symptoms persist"},
        ],
        "red_flags": [],
        "doctor": {"specialty": "GENERAL_MEDICINE", "when": "within a week"},
        "follow_up_questions": [f"Useful question {number}?" for number in range(question_count)],
        "disclaimer": "DISCLAIMER: This is not a diagnosis.",
    }


class FakeLlm:
    def __init__(self, responses):
        self.responses = iter(responses)
        self.prompts = []

    def invoke(self, prompt):
        self.prompts.append(prompt)
        return SimpleNamespace(content=next(self.responses))


class StructuredAnswerParsingTests(unittest.TestCase):
    def test_valid_response_with_two_questions(self):
        result = parse_structured_answer(json.dumps(answer_payload(2)))
        self.assertEqual(len(result.follow_up_questions), 2)

    def test_valid_response_with_three_questions(self):
        result = parse_structured_answer(json.dumps(answer_payload(3)))
        self.assertEqual(len(result.follow_up_questions), 3)

    def test_json_is_extracted_from_wrapping_text(self):
        wrapped = f"```json\n{json.dumps(answer_payload(2))}\n```"
        result = parse_structured_answer(wrapped)
        self.assertEqual(len(result.follow_up_questions), 2)

    def test_five_questions_are_normalized_to_three(self):
        result = parse_structured_answer(json.dumps(answer_payload(5)))
        self.assertEqual(result.follow_up_questions, answer_payload(5)["follow_up_questions"][:3])

    def test_malformed_json_uses_one_repair_attempt(self):
        service = RagService.__new__(RagService)
        service.llm = FakeLlm([json.dumps(answer_payload(2))])

        result = service._parse_answer_with_one_repair("{not valid json")

        self.assertEqual(len(result.follow_up_questions), 2)
        self.assertEqual(len(service.llm.prompts), 1)

    def test_repair_response_with_five_questions_is_normalized(self):
        service = RagService.__new__(RagService)
        service.llm = FakeLlm([json.dumps(answer_payload(5))])

        result = service._parse_answer_with_one_repair("malformed")

        self.assertEqual(len(result.follow_up_questions), 3)
        self.assertEqual(len(service.llm.prompts), 1)

    def test_successful_first_parse_does_not_attempt_repair(self):
        service = RagService.__new__(RagService)
        service.llm = FakeLlm([])

        result = service._parse_answer_with_one_repair(json.dumps(answer_payload(2)))

        self.assertEqual(len(result.follow_up_questions), 2)
        self.assertEqual(service.llm.prompts, [])

    def test_invalid_response_after_repair_fails_cleanly(self):
        service = RagService.__new__(RagService)
        service.llm = FakeLlm(["still not JSON"])

        with self.assertRaisesRegex(
            ValueError, "remained invalid after one repair attempt"
        ):
            service._parse_answer_with_one_repair("not JSON")

        self.assertEqual(len(service.llm.prompts), 1)


if __name__ == "__main__":
    unittest.main()
