import { Component, OnInit } from '@angular/core';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-mfa-setup-page',
  imports: [FormsModule, NgIf, RouterLink],
  templateUrl: './mfa-setup-page.html',
  styleUrl: './mfa-setup-page.scss'
})
export class MfaSetupPage implements OnInit {
  otpCode = '';
  qrCodeImageUri: string | null = null;
  challengeToken = '';
  isLoading = false;
  errorMessage = '';

  constructor(private authService: Authentication, private router: Router) {}

  ngOnInit(): void {
    const pending = this.authService.getMfaChallenge();
    const navigationState = typeof history !== 'undefined' ? history.state : null;
    this.qrCodeImageUri = navigationState?.qrCodeImageUri || null;
    if (pending?.enrollment) {
      this.challengeToken = pending.challengeToken;
      if (!this.qrCodeImageUri) this.resumeEnrollment();
      return;
    }
    if (this.authService.isLoggedIn()) {
      this.startAuthenticatedEnrollment();
      return;
    }
    this.router.navigate(['/']);
  }

  normalizeCode(): void {
    this.otpCode = this.otpCode.replace(/\D/g, '').slice(0, 6);
  }

  verifyCode(): void {
    this.normalizeCode();
    if (this.isLoading || !/^\d{6}$/.test(this.otpCode)) {
      this.errorMessage = 'Enter a valid 6-digit verification code.';
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.verifyEnrollmentCode({ challengeToken: this.challengeToken, code: this.otpCode })
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          if (!this.authService.storeSession(response)) {
            this.errorMessage = 'Setup succeeded but no valid session was returned.';
            return;
          }
          this.redirectBasedOnRole();
        },
        error: (error) => this.handleError(error)
      });
  }

  cancel(): void {
    this.authService.clearMfaChallenge();
    this.router.navigate(['/']);
  }

  private resumeEnrollment(): void {
    this.isLoading = true;
    this.authService.resumeEnrollment(this.challengeToken).subscribe({
      next: (response) => this.acceptEnrollmentResponse(response),
      error: (error) => this.handleError(error)
    });
  }

  private startAuthenticatedEnrollment(): void {
    this.isLoading = true;
    this.authService.beginEnrollment().subscribe({
      next: (response) => this.acceptEnrollmentResponse(response),
      error: (error) => this.handleError(error)
    });
  }

  private acceptEnrollmentResponse(response: AuthenticationResponse): void {
    this.isLoading = false;
    if (!response.qrCodeImageUri || !this.authService.beginMfa(response)) {
      this.errorMessage = 'Unable to start authenticator setup. Please try again.';
      return;
    }
    this.qrCodeImageUri = response.qrCodeImageUri;
    this.challengeToken = response.mfaChallengeToken!;
  }

  private handleError(error: any): void {
    this.isLoading = false;
    this.otpCode = '';
    const code = error?.error?.error;
    if (code === 'MFA_CHALLENGE_EXPIRED') this.authService.clearMfaChallenge();
    this.errorMessage = code === 'INVALID_MFA_CODE'
      ? 'That code is invalid. Enter the current code from your authenticator app.'
      : (error?.error?.message || 'Authenticator setup could not be completed. Please sign in again.');
  }

  private redirectBasedOnRole(): void {
    const destinations: Record<string, string> = {
      ADMIN: '/admin-dashboard/overview',
      DOCTOR: '/doctor-dashboard/overview',
      PATIENT: '/patient-dashboard'
    };
    this.router.navigate([destinations[this.authService.getUserRole() || ''] || '/welcome']);
  }
}
