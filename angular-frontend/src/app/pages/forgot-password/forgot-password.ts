import { NgIf } from '@angular/common';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-forgot-password',
  imports: [NgIf, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: '../password-auth.scss'
})
export class ForgotPassword {
  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(100)]
    })
  });
  loading = false;
  submitted = false;
  success = false;
  errorMessage = '';

  constructor(private authService: Authentication) {}

  submit(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.form.invalid || this.loading) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.authService.forgotPassword(this.form.controls.email.value.trim()).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = "We couldn't send the reset email. Please try again later.";
      }
    });
  }
}
