import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { finalize } from 'rxjs/operators';
import { PatientDto } from '../../models/PatientDto';
import { PatientService } from '../../services/patient/patient-service';

@Component({
  selector: 'app-patient-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-profile-page.html',
  styleUrl: './patient-profile-page.scss',
})
export class PatientProfilePage implements OnInit {
  private fb = inject(FormBuilder);
  private patientService = inject(PatientService);

  loading = true;
  saving = false;
  error: string | null = null;
  success: string | null = null;

  form = this.fb.group({
    firstname: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    lastname: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: [{ value: '', disabled: true }, [Validators.email, Validators.maxLength(100)]],
    phone: ['', [Validators.maxLength(30)]],
    dateOfBirth: [''], // yyyy-mm-dd
    gender: [''],      // free string Ø£Ùˆ dropdown
  });

  ngOnInit(): void {
    this.fetchMe();
  }

  fetchMe(): void {
    this.loading = true;
    this.error = null;
    this.success = null;

    this.patientService
      .getMe()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (me) => this.patchForm(me),
        error: (err) => {
          this.error = err?.error?.message || 'Failed to load profile.';
        },
      });
  }

  patchForm(me: PatientDto): void {
    this.form.patchValue({
      firstname: me.firstname ?? '',
      lastname: me.lastname ?? '',
      email: me.email ?? '',
      phone: me.phone ?? '',
      dateOfBirth: me.dateOfBirth ?? '',
      gender: me.gender ?? '',
    });
  }

  save(): void {
    this.success = null;
    this.error = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: PatientDto = {
      firstname: this.form.value.firstname ?? '',
      lastname: this.form.value.lastname ?? '',
      // email non envoyÃ© (readonly + backend ignore anyway)
      phone: this.form.value.phone ?? '',
      dateOfBirth: this.form.value.dateOfBirth || undefined,
      gender: this.form.value.gender ?? '',
    };

    this.saving = true;
    this.patientService
      .updateMe(payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (updated) => {
          this.patchForm(updated);
          this.success = 'Profile updated successfully.';
        },
        error: (err) => {
          this.error = err?.error?.message || 'Failed to update profile.';
        },
      });
  }

  // Helpers UI
  c(name: string) {
    return this.form.get(name);
  }
}

