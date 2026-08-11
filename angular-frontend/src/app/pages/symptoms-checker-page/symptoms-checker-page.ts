import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { ChatResponse, PrediagnosticApiService } from '../../services/prediagnostic/prediagnostic-api.service.ts';

interface Message {
  role: 'human' | 'ai';
  content: string;
  response?: ChatResponse['answer'];
}

@Component({
  selector: 'app-symptoms-checker-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './symptoms-checker-page.html',
  styleUrl: './symptoms-checker-page.scss'
})
export class SymptomsCheckerPage {
  input = '';
  loading = false;
  errorMessage = '';
  lastFailedMessage = '';

  sessionId = this.createSessionId();

  messages: Message[] = [];

  constructor(private api: PrediagnosticApiService, private router: Router) {
    if (typeof sessionStorage !== 'undefined') {
      this.sessionId = sessionStorage.getItem('ai_session_id') ?? this.sessionId;
      sessionStorage.setItem('ai_session_id', this.sessionId);
    }
  }

  send() {
    const message = this.input.trim();
    if (!message || this.loading || message.length > 2000) return;

    this.input = '';
    this.messages.push({ role: 'human', content: message });
    this.requestAnalysis(message);
  }

  retry(): void {
    if (!this.lastFailedMessage || this.loading) return;
    this.requestAnalysis(this.lastFailedMessage);
  }

  newConversation(): void {
    if (this.loading) return;
    this.sessionId = this.createSessionId();
    this.messages = [];
    this.errorMessage = '';
    this.lastFailedMessage = '';
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem('ai_session_id', this.sessionId);
    }
  }

  findSpecialist(specialty: string): void {
    const canonical = this.normalizeSpecialty(specialty);
    this.router.navigate(['/patient-dashboard/find-doctor'], {
      queryParams: { specialty: canonical }
    });
  }

  displaySpecialty(value: string): string {
    return value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  private requestAnalysis(userMessage: string): void {
    this.errorMessage = '';
    this.loading = true;

    this.api.chat({
      session_id: this.sessionId,
      message: userMessage
    }).pipe(finalize(() => (this.loading = false))).subscribe({
      next: (res) => {
        this.messages.push({
          role: 'ai',
          content: res.answer.chief_summary,
          response: res.answer
        });
        this.lastFailedMessage = '';
      },
      error: () => {
        this.lastFailedMessage = userMessage;
        this.errorMessage = 'The medical assistant is unavailable right now. Please try again.';
      }
    });
  }

  private createSessionId(): string {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID();
    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  private normalizeSpecialty(value: string): string {
    return value.trim().replace(/[\s-]+/g, '_').replace(/[^A-Za-z0-9_]/g, '').toUpperCase();
  }
  onKeydown(event: Event) {
  const e = event as KeyboardEvent;

  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    this.send();
  }
}

}

