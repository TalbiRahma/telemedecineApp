import { Component } from '@angular/core';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthenticationRequest } from '../../models/auth/authentication-request';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-login',
  imports: [NgIf, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  authRequest: AuthenticationRequest = { email: '', password: '' };
  showPassword = false;
  loginError = '';
  isLoading = false;

  constructor(
    private authService: Authentication,
    private router: Router
  ) {}

  authenticate(): void {
    if (this.isLoading) return;
    this.loginError = '';
    this.authService.clearMfaChallenge();
    this.isLoading = true;

    this.authService.login(this.authRequest).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.mfaRequired) {
          if (!this.authService.beginMfa(response)) {
            this.loginError = 'Unable to start two-factor verification. Please sign in again.';
            return;
          }
          this.router.navigate([
            response.mfaEnrollmentRequired ? '/mfa-setup' : '/two-fa'
          ]);
          return;
        }

        if (!this.authService.storeSession(response)) {
          this.loginError = 'Sign-in did not return a valid session. Please try again.';
          return;
        }
        this.redirectBasedOnRole();
      },
      error: () => {
        this.isLoading = false;
        this.loginError = 'Invalid email or password. Please try again.';
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
