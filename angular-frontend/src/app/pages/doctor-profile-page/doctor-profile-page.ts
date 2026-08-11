import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Authentication } from '../../services/auth/authentication';
import { Doctor } from '../../models/doctor';
import { DoctorService } from '../../services/doctor/doctor-service';

@Component({
  selector: 'app-doctor-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], // ✅ FormBuilder removed
  templateUrl: './doctor-profile-page.html',
  styleUrl: './doctor-profile-page.scss',
})
export class DoctorProfilePage implements OnInit {
  loading = true;
  saving = false;
  error = '';
  success = '';
  showCertificationEditor = false;
  doctor?: Doctor;

  form!: FormGroup; // ✅ created after DI

  constructor(
    private fb: FormBuilder,
    private doctorService: DoctorService,
    private auth: Authentication
  ) {
    // ✅ create form here (fb is available)
    this.form = this.fb.group({
      firstname: ['', [Validators.required, Validators.minLength(2)]],
      lastname: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      licenseNumber: [''],
      adresse: [''],
      certificationUrl: [''],
    });
  }

  ngOnInit(): void {
    const doctorId = this.getDoctorId();

    this.doctorService.getDoctorById(doctorId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (d) => {
          this.doctor = d;
          this.form.patchValue({
            firstname: d.firstname,
            lastname: d.lastname,
            email: d.email,
            phone: d.phone,                // ✅ add phone here
            licenseNumber: d.licenseNumber,
            adresse: d.adresse,
            certificationUrl: d.certificationUrl
          });
        },
        error: (e) => this.error = e?.error?.message || 'Failed to load profile'
      });
  }

  save(): void {
    if (!this.doctor?.id) return;

    this.error = '';
    this.success = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;

    const payload = this.form.value as Partial<Doctor>;

    this.doctorService.updateDoctor(this.doctor.id, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (updated) => {
          this.doctor = updated;
          this.success = 'Your profile has been updated successfully.';
          this.form.patchValue({
            firstname: updated.firstname,
            lastname: updated.lastname,
            email: updated.email,
            phone: updated.phone,
            licenseNumber: updated.licenseNumber,
            adresse: updated.adresse,
            certificationUrl: updated.certificationUrl
          });
        },
        error: (e) => this.error = e?.error?.message || 'Failed to save profile'
      });
  }

  private getDoctorId(): number {
    const id = (this.auth as any)?.getUserId?.() ?? localStorage.getItem('userId');
    const parsed = Number(id);
    if (!parsed) throw new Error('Doctor id not found. Store it at login or use /me endpoint.');
    return parsed;
  }

  badgeClass(state?: string) {
    switch (state) {
      case 'CONFIRMED': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
      case 'REJECTED':  return 'bg-rose-100 text-rose-700 border-rose-200';
      default:          return 'bg-amber-100 text-amber-800 border-amber-200';
    }
  }

  get doctorInitials(): string {
    const first = this.doctor?.firstname?.charAt(0) || '';
    const last = this.doctor?.lastname?.charAt(0) || '';
    return (first + last).toUpperCase() || 'DR';
  }
}
