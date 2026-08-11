// src/app/pages/doctor-appointments/doctor-appointments.ts
import { Component, OnInit } from '@angular/core';
import { DatePipe, NgClass, NgFor, NgIf, SlicePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AppointmentService } from '../../services/appointement/appointment-service';
import { Authentication } from '../../services/auth/authentication';
import { PatientService } from '../../services/patient/patient-service';

import { Appointment } from '../../models/Appointment ';
import { PatientDto } from '../../models/PatientDto';

type AppointmentStatusUi = 'BOOKED' | 'CONFIRMED' | 'CANCELED' | 'COMPLETED' | 'NO_SHOW';

@Component({
  selector: 'app-doctor-appointments',
  standalone: true,
  imports: [DatePipe, NgFor, NgClass, SlicePipe, NgIf, RouterLink],
  templateUrl: './doctor-appointments.html',
  styleUrl: './doctor-appointments.scss'
})
export class DoctorAppointments implements OnInit {

  appointments: Appointment[] = [];
  groupedByDate = new Map<string, Appointment[]>();

  selectedDate: Date = new Date();
  monthDays: Date[] = [];

  doctorId: number | null = null;

  loading = false;
  errorMsg = '';

  // ✅ modal
  detailsOpen = false;
  selectedAppt: Appointment | null = null;

  // ✅ patient details in modal
  patientLoading = false;
  patientError = '';
  patientDetails: PatientDto | null = null;

  constructor(
    private appointmentService: AppointmentService,
    private authService: Authentication,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.doctorId = this.authService.getUserId();
    this.buildMonth(this.selectedDate);
    this.loadAppointments();
  }

  loadAppointments() {
    if (!this.doctorId) return;

    this.loading = true;
    this.errorMsg = '';
    this.closeDetails();

    this.appointmentService.getByDoctor(this.doctorId).subscribe({
      next: (data) => {
        this.appointments = (data || []).slice();

        this.appointments.sort((a, b) =>
          (a.slotStartDateTime || '').localeCompare(b.slotStartDateTime || '')
        );

        this.groupAppointments();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Failed to load appointments.';
        this.loading = false;
      }
    });
  }

  // ✅ modal handlers
  openDetails(a: Appointment) {
    this.selectedAppt = a;
    this.detailsOpen = true;

    // reset patient panel
    this.patientDetails = null;
    this.patientError = '';
    this.patientLoading = false;

    const pid = a.patientId;
    if (!pid) return;

    this.patientLoading = true;
    this.patientService.getById(pid).subscribe({
      next: (p) => {
        this.patientDetails = p;
        this.patientLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.patientError = 'Failed to load patient details.';
        this.patientLoading = false;
      }
    });
  }

  closeDetails() {
    this.detailsOpen = false;
    this.selectedAppt = null;

    this.patientDetails = null;
    this.patientError = '';
    this.patientLoading = false;
  }

  // ✅ prevents null errors in template
  get patientDisplayName(): string {
    const fn = this.patientDetails?.firstname?.trim();
    const ln = this.patientDetails?.lastname?.trim();
    const full = `${fn ?? ''} ${ln ?? ''}`.trim();
    return full || (this.selectedAppt ? this.patientLabel(this.selectedAppt) : 'Patient');
  }

  // Calendar build
  buildMonth(reference: Date) {
    const year = reference.getFullYear();
    const month = reference.getMonth();

    const first = new Date(year, month, 1);
    const start = new Date(first);
    start.setDate(first.getDate() - first.getDay()); // Sunday start

    this.monthDays = [];
    for (let i = 0; i < 42; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      this.monthDays.push(d);
    }
  }

  prevMonth() {
    const d = new Date(this.selectedDate);
    d.setMonth(d.getMonth() - 1);
    this.selectedDate = d;
    this.buildMonth(d);
  }

  nextMonth() {
    const d = new Date(this.selectedDate);
    d.setMonth(d.getMonth() + 1);
    this.selectedDate = d;
    this.buildMonth(d);
  }

  selectDay(d: Date) {
    this.selectedDate = d;
  }

  appointmentsFor(d: Date): Appointment[] {
    const key = this.toDateKey(d);
    return this.groupedByDate.get(key) || [];
  }

  isSameMonth(d: Date) {
    return d.getMonth() === this.selectedDate.getMonth();
  }

  hasStatus(day: Date, status: AppointmentStatusUi): boolean {
    const st = status.toUpperCase();
    return this.appointmentsFor(day).some(a => (a.status || '').toUpperCase() === st);
  }

  private groupAppointments() {
    this.groupedByDate = new Map<string, Appointment[]>();

    for (const a of this.appointments) {
      const startIso = a.slotStartDateTime;
      if (!startIso) continue;

      const d = new Date(startIso);
      if (Number.isNaN(d.getTime())) continue;

      const key = this.toDateKey(d);
      if (!this.groupedByDate.has(key)) this.groupedByDate.set(key, []);
      this.groupedByDate.get(key)!.push(a);
    }

    for (const [key, list] of this.groupedByDate.entries()) {
      list.sort((x, y) =>
        (x.slotStartDateTime || '').localeCompare(y.slotStartDateTime || '')
      );
      this.groupedByDate.set(key, list);
    }
  }

  // --- display helpers ---
  startTime(a: Appointment): string {
    return (a.slotStartDateTime || '').slice(11, 16) || '—';
  }

  endTime(a: Appointment): string {
    return (a.slotEndDateTime || '').slice(11, 16) || '—';
  }

  patientLabel(a: Appointment): string {
    return a.patientName || `Patient #${a.patientId ?? '—'}`;
  }

  statusLabel(a: Appointment): string {
    const st = ((a.status || '') as string).toUpperCase();
    return st || '—';
  }

  statusClass(a: Appointment): string {
    const st = ((a.status || '') as string).toUpperCase();
    return st === 'BOOKED' ? 'bg-amber-100 text-amber-800'
      : st === 'CONFIRMED' ? 'bg-blue-100 text-blue-800'
      : st === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800'
      : st === 'CANCELED' ? 'bg-rose-100 text-rose-800'
      : st === 'NO_SHOW' ? 'bg-slate-100 text-slate-700'
      : 'bg-slate-100 text-slate-700';
  }

  // --- actions ---
  canComplete(a: Appointment): boolean {
    const st = ((a.status || '') as string).toUpperCase();
    return st === 'CONFIRMED' || st === 'BOOKED';
  }

  canCancel(a: Appointment): boolean {
    const st = ((a.status || '') as string).toUpperCase();
    return st === 'CONFIRMED' || st === 'BOOKED';
  }

  canNoShow(a: Appointment): boolean {
    const st = ((a.status || '') as string).toUpperCase();
    return st === 'CONFIRMED' || st === 'BOOKED';
  }

  cancelAppointment(a: Appointment) {
    if (!a.id) return;
    this.loading = true;

    this.appointmentService.cancel(a.id).subscribe({
      next: () => this.loadAppointments(),
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Failed to cancel.';
        this.loading = false;
      }
    });
  }

  completeAppointment(a: Appointment) {
    if (!a.id) return;
    this.loading = true;

    this.appointmentService.complete(a.id).subscribe({
      next: () => this.loadAppointments(),
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Failed to complete.';
        this.loading = false;
      }
    });
  }

  markNoShowAppointment(a: Appointment) {
    if (!a.id) return;
    this.loading = true;

    this.appointmentService.noShow(a.id).subscribe({
      next: () => this.loadAppointments(),
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Failed to mark no-show.';
        this.loading = false;
      }
    });
  }

  private toDateKey(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }
}
