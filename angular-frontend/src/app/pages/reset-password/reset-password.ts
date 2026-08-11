import { NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-reset-password',
  imports: [NgIf, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: '../password-auth.scss'
})
export class ResetPassword implements OnInit {
  readonly form = new FormGroup({
    newPassword: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8), Validators.maxLength(255)] }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8), Validators.maxLength(255)] })
  });
  token = '';
  loading = false;
  submitted = false;
  success = false;
  invalidToken = false;
  errorMessage = '';
  showPassword = false;
  showConfirmation = false;

  constructor(private route: ActivatedRoute, private authService: Authentication) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token')?.trim() ?? '';
    this.invalidToken = !this.token;
  }

  get passwordsMismatch(): boolean {
    const { newPassword, confirmPassword } = this.form.getRawValue();
    return !!confirmPassword && newPassword !== confirmPassword;
  }

  submit(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.invalidToken || this.form.invalid || this.passwordsMismatch || this.loading) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    const values = this.form.getRawValue();
    this.authService.resetPassword(this.token, values.newPassword, values.confirmPassword).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
      },
      error: (error) => {
        this.loading = false;
        if (error?.error?.error === 'INVALID_RESET_TOKEN') {
          this.invalidToken = true;
          this.errorMessage = '';
        } else {
          this.errorMessage = error?.error?.message || 'Unable to reset your password. Please try again.';
        }
      }
    });
  }
}
