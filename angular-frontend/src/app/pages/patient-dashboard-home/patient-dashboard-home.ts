import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { PatientService } from '../../services/patient/patient-service';

import { PatientDto } from '../../models/PatientDto';
import { AppointmentService } from '../../services/appointement/appointment-service';
import { Appointment, AppointmentStatus } from '../../models/Appointment ';

@Component({
  selector: 'app-patient-dashboard-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './patient-dashboard-home.html',
  styleUrl: './patient-dashboard-home.scss',
})
export class PatientDashboardHome implements OnInit {
  private patientService = inject(PatientService);
  private appointmentService = inject(AppointmentService);

  // ✅ Type-safe state
  state: 'loading' | 'ready' | 'error' = 'loading';
  errorMessage = '';

  patient: PatientDto | null = null;
  nextAppointment: Appointment | null = null;
  tip = '';

  private tips = [
    'Stay hydrated and avoid skipping meals before a consultation. Consistent habits help improve diagnostic accuracy.',
    'Write down your symptoms and medications before your appointment.',
    'Keep your lab results ready — it helps your doctor make faster decisions.',
    'Try to sleep well before your visit. Rest improves recovery and accuracy.',
  ];

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.state = 'loading';
    this.errorMessage = '';
    this.patient = null;
    this.nextAppointment = null;
    this.tip = this.randomTip();

    // 1) Load patient
    this.patientService.getMe().subscribe({
      next: (p) => {
        this.patient = p;

        if (!p?.id) {
          this.state = 'error';
          this.errorMessage = 'Patient id not found.';
          return;
        }

        // 2) Load patient appointments
        this.appointmentService.getMine().subscribe({
          next: (apps) => {
            this.nextAppointment = this.pickNextAppointment(apps || []);
            this.state = 'ready';
          },
          error: () => {
            this.state = 'error';
            this.errorMessage = 'We could not load your appointments.';
          },
        });
      },
      error: (err) => {
        this.state = 'error';
        this.errorMessage = err?.error?.message || 'Failed to load dashboard.';
      },
    });
  }

  // ✅ pick nearest upcoming BOOKED/CONFIRMED
  private pickNextAppointment(list: Appointment[]): Appointment | null {
    if (!list?.length) return null;

    const allowed: AppointmentStatus[] = ['BOOKED', 'CONFIRMED'];
    const now = Date.now();

    const upcoming = list
      .filter((a) => allowed.includes(a.status as AppointmentStatus))
      .filter((a) => {
        const startIso = a.slotStartDateTime;
        if (!startIso) return false;
        const start = new Date(startIso).getTime();
        return !Number.isNaN(start) && start >= now;
      })
      .sort(
        (a, b) =>
          new Date(a.slotStartDateTime!).getTime() -
          new Date(b.slotStartDateTime!).getTime()
      );

    return upcoming[0] ?? null;
  }

  formatDateTime(iso?: string): string {
    if (!iso) return '';
    const d = new Date(iso);

    return d
      .toLocaleString('en-GB', {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
      .replace(',', ' –');
  }

  doctorFullName(a: Appointment): string {
    const fn = a.doctorFirstname ?? '';
    const ln = a.doctorLastname ?? '';
    const name = `${fn} ${ln}`.trim();
    return name ? `Dr. ${name}` : 'Doctor';
  }

  doctorSpecialty(a: Appointment): string {
    return a.doctorSpecialtyName ?? '';
  }

  private randomTip(): string {
    return this.tips[Math.floor(Math.random() * this.tips.length)];
  }
}
