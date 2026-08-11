import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ChatRequest {
  session_id: string;
  message: string;
}

export interface ChatResponse {
  session_id: string;
  answer: {
    chief_summary: string;
    differential: {
      condition: string;
      likelihood: 'LOW' | 'MEDIUM' | 'HIGH';
      rationale: string;
    }[];
    urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'EMERGENCY';
    urgency_reason: string;
    next_steps: { category: string; text: string }[];
    red_flags: { symptom: string; action: string }[];
    doctor: { specialty: string; when: string };
    follow_up_questions: string[];
    disclaimer: string;
  };
  summary: string;
}

@Injectable({ providedIn: 'root' })
export class PrediagnosticApiService {
  private readonly baseUrl = '/api/v1/ai';

  constructor(private http: HttpClient) {}

  chat(payload: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, payload);
  }
}
