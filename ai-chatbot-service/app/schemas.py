from pydantic import BaseModel, Field
from typing import List, Literal, Optional

UrgencyLevel = Literal["LOW", "MEDIUM", "HIGH", "EMERGENCY"]
Likelihood = Literal["LOW", "MEDIUM", "HIGH"]

class DifferentialItem(BaseModel):
    condition: str = Field(..., min_length=2)
    likelihood: Likelihood
    rationale: str = Field(..., min_length=5)

class RedFlag(BaseModel):
    symptom: str
    action: str  # e.g., "Go to ER now", "Call emergency services", ...

class NextStep(BaseModel):
    category: Literal["SELF_CARE", "TESTS", "MEDICATION_SAFETY", "FOLLOW_UP"]
    text: str

class DoctorRecommendation(BaseModel):
    specialty: str
    when: str  # e.g., "within 24h", "today", "within a week"

class StructuredAnswer(BaseModel):
    chief_summary: str
    differential: List[DifferentialItem] = Field(..., min_length=2, max_length=5)
    urgency: UrgencyLevel
    urgency_reason: str
    next_steps: List[NextStep] = Field(..., min_length=2, max_length=8)
    red_flags: List[RedFlag] = Field(default_factory=list)
    doctor: DoctorRecommendation
    follow_up_questions: List[str] = Field(..., min_length=2, max_length=3)
    disclaimer: str

class ChatRequest(BaseModel):
    session_id: str = Field(..., min_length=1, max_length=64, pattern=r"^[A-Za-z0-9_-]+$")
    message: str = Field(..., min_length=1, max_length=2000)

class ChatResponse(BaseModel):
    session_id: str
    answer: StructuredAnswer
    summary: str
