import { Component, OnInit } from '@angular/core';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-two-fa-page',
  imports: [FormsModule, NgIf, RouterLink],
  templateUrl: './two-fa-page.html',
  styleUrl: './two-fa-page.scss'
})
export class TwoFaPage implements OnInit {
  otpCode = '';
  isLoading = false;
  isRecovering = false;
  errorMessage = '';
  challengeToken = '';

  constructor(
    private authService: Authentication,
    private router: Router
  ) {}

  ngOnInit(): void {
    const pending = this.authService.getMfaChallenge();
    if (!pending) {
      this.router.navigate(['/']);
      return;
    }
    this.challengeToken = pending.challengeToken;
    if (pending.enrollment) {
      this.router.navigate(['/mfa-setup']);
    }
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
    this.authService.verifyCode({
      challengeToken: this.challengeToken,
      code: this.otpCode
    }).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (!this.authService.storeSession(response)) {
          this.errorMessage = 'Verification succeeded but no valid session was returned.';
          return;
        }
        this.redirectBasedOnRole();
      },
      error: (error) => {
        this.isLoading = false;
        this.otpCode = '';
        const errorCode = error?.error?.error;
        if (errorCode === 'MFA_CHALLENGE_EXPIRED') {
          this.authService.clearMfaChallenge();
          this.errorMessage = 'Your verification session has expired. Please sign in again.';
        } else if (errorCode === 'MFA_TOO_MANY_ATTEMPTS') {
          this.authService.clearMfaChallenge();
          this.errorMessage = 'Too many verification attempts. Please sign in again.';
        } else {
          this.errorMessage = 'Invalid or expired verification code.';
        }
      }
    });
  }

  backToSignIn(): void {
    this.authService.clearMfaChallenge();
    this.router.navigate(['/']);
  }

  setUpAuthenticatorAgain(): void {
    if (this.isLoading || this.isRecovering || !this.challengeToken) return;
    this.isRecovering = true;
    this.errorMessage = '';
    this.authService.resumeEnrollment(this.challengeToken).subscribe({
      next: (response) => {
        this.isRecovering = false;
        if (!response.qrCodeImageUri || !this.authService.beginMfa(response)) {
          this.errorMessage = 'Unable to start authenticator setup. Please sign in again.';
          return;
        }
        this.router.navigate(['/mfa-setup'], {
          state: { qrCodeImageUri: response.qrCodeImageUri }
        });
      },
      error: (error) => {
        this.isRecovering = false;
        const errorCode = error?.error?.error;
        if (errorCode === 'MFA_CHALLENGE_EXPIRED' || errorCode === 'MFA_TOO_MANY_ATTEMPTS') {
          this.authService.clearMfaChallenge();
          this.errorMessage = 'Your verification session has expired. Please sign in again.';
        } else {
          this.errorMessage = 'Authenticator setup could not be started. Please sign in again.';
        }
      }
    });
  }

  private redirectBasedOnRole(): void {
    switch (this.authService.getUserRole()) {
      case 'ADMIN':
        this.router.navigate(['/admin-dashboard/overview']);
        break;
      case 'DOCTOR':
        this.router.navigate(['/doctor-dashboard/overview']);
        break;
      case 'PATIENT':
        this.router.navigate(['/patient-dashboard']);
        break;
      default:
        this.router.navigate(['/welcome']);
    }
  }
}
