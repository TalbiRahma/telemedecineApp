// doctor-overview.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { Authentication } from '../../services/auth/authentication';
import { DoctorService } from '../../services/doctor/doctor-service';
import { AppointmentService } from '../../services/appointement/appointment-service';
import { PatientService } from '../../services/patient/patient-service';

import { Doctor } from '../../models/doctor';
import { Appointment, AppointmentStatus } from '../../models/Appointment ';
import { PatientDto } from '../../models/PatientDto';

type UiAppt = Appointment & {
  dateIso: string;     // YYYY-MM-DD
  timeLabel: string;   // HH:mm — HH:mm
  statusClass: string;
  patientLabel: string;

  canCancel: boolean;
  canComplete: boolean;
  canNoShow: boolean;
};

@Component({
  selector: 'app-doctor-overview',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './doctor-overview.html',
  styleUrl: './doctor-overview.scss'
})
export class DoctorOverview implements OnInit {

  doctorId!: number;
  doctor: Doctor | null = null;

  loading = true;
  errorMsg = '';
  actionMsg = '';

  stats = {
    appointmentsToday: 0,
    appointmentsThisWeek: 0,
    totalAppointments: 0,
    confirmed: 0,
    completed: 0,
    canceled: 0,
    noShow: 0
  };

  todayAppointments: UiAppt[] = [];
  nextAppointment: UiAppt | null = null;

  // Pagination (Today list)
  todayPage = 1;
  todayPageSize = 5;

  // Modal state
  detailsOpen = false;
  selectedAppt: UiAppt | null = null;

  // Patient details in modal
  patientLoading = false;
  patientError = '';
  patientDetails: PatientDto | null = null;

  constructor(
    private auth: Authentication,
    private doctorService: DoctorService,
    private appointmentService: AppointmentService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    const id = this.auth.getUserId();
    if (!id) {
      this.errorMsg = 'Doctor id not found. Please login again.';
      this.loading = false;
      return;
    }
    this.doctorId = id;
    this.refresh();
  }

  refresh() {
    this.loading = true;
    this.errorMsg = '';
    this.actionMsg = '';
    this.todayAppointments = [];
    this.nextAppointment = null;

    this.closeDetails();

    // 1) doctor info
    this.doctorService.getDoctorById(this.doctorId).subscribe({
      next: (d) => (this.doctor = d),
      error: (err) => console.error(err)
    });

    // 2) appointments
    this.appointmentService.getByDoctor(this.doctorId).subscribe({
      next: (apps) => {
        const list = (apps || []).slice();

        const ui = list
          .filter(a => !!a.slotStartDateTime)
          .map(a => this.toUi(a))
          .sort((a, b) => (a.slotStartDateTime || '').localeCompare(b.slotStartDateTime || ''));

        this.computeStats(ui);
        this.buildToday(ui);
        this.buildNext(ui);

        this.clampTodayPage();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Failed to load appointments.';
        this.loading = false;
      }
    });
  }

  // -----------------------
  // Modal handlers
  // -----------------------
  openDetails(a: UiAppt) {
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

  // -------- actions ----------
  complete(id?: number) {
    if (!id) return;
    this.actionMsg = '';
    this.appointmentService.complete(id).subscribe({
      next: () => {
        this.actionMsg = 'Marked as completed ✅';
        this.refresh();
      },
      error: (err) => {
        console.error(err);
        this.actionMsg = err?.error?.message || 'Failed to complete.';
      }
    });
  }

  noShow(id?: number) {
    if (!id) return;
    this.actionMsg = '';
    this.appointmentService.noShow(id).subscribe({
      next: () => {
        this.actionMsg = 'Marked as no-show ✅';
        this.refresh();
      },
      error: (err) => {
        console.error(err);
        this.actionMsg = err?.error?.message || 'Failed to mark no-show.';
      }
    });
  }

  cancel(id?: number) {
    if (!id) return;
    this.actionMsg = '';
    this.appointmentService.cancel(id).subscribe({
      next: () => {
        this.actionMsg = 'Appointment canceled ✅';
        this.refresh();
      },
      error: (err) => {
        console.error(err);
        this.actionMsg = err?.error?.message || 'Failed to cancel.';
      }
    });
  }

  // -------- computed helpers ----------
  get doctorFullName(): string {
    if (!this.doctor) return 'Doctor';
    const full = `${this.doctor.firstname || ''} ${this.doctor.lastname || ''}`.trim();
    return full ? `Dr. ${full}` : 'Doctor';
  }

  get doctorInitials(): string {
    const d = this.doctor;
    if (!d) return 'DR';
    const f = (d.firstname || '').trim().charAt(0).toUpperCase();
    const l = (d.lastname || '').trim().charAt(0).toUpperCase();
    return (f + l).trim() || 'DR';
  }

  // ✅ used in modal header (prevents null template errors)
  get patientDisplayName(): string {
    const fn = this.patientDetails?.firstname?.trim();
    const ln = this.patientDetails?.lastname?.trim();
    const full = `${fn ?? ''} ${ln ?? ''}`.trim();
    return full || this.selectedAppt?.patientLabel || 'Patient';
  }

  // -----------------------
  // Pagination getters
  // -----------------------
  get todayTotalPages(): number {
    const total = this.todayAppointments?.length || 0;
    return Math.max(1, Math.ceil(total / this.todayPageSize));
  }

  get todayStartIndex(): number {
    return (this.todayPage - 1) * this.todayPageSize;
  }

  get todayEndIndex(): number {
    const total = this.todayAppointments?.length || 0;
    return Math.min(total, this.todayStartIndex + this.todayPageSize);
  }

  get pagedTodayAppointments(): UiAppt[] {
    return (this.todayAppointments || []).slice(this.todayStartIndex, this.todayEndIndex);
  }

  prevTodayPage() {
    this.todayPage = Math.max(1, this.todayPage - 1);
  }

  nextTodayPage() {
    this.todayPage = Math.min(this.todayTotalPages, this.todayPage + 1);
  }

  private clampTodayPage() {
    this.todayPage = Math.min(this.todayPage, this.todayTotalPages);
    this.todayPage = Math.max(1, this.todayPage);
  }

  // -----------------------
  // Build stats & lists
  // -----------------------
  private computeStats(list: UiAppt[]) {
    this.stats.totalAppointments = list.length;
    this.stats.confirmed = list.filter(a => this.eq(a.status, 'CONFIRMED')).length;
    this.stats.completed = list.filter(a => this.eq(a.status, 'COMPLETED')).length;
    this.stats.canceled = list.filter(a => this.eq(a.status, 'CANCELED')).length;
    this.stats.noShow = list.filter(a => this.eq(a.status, 'NO_SHOW')).length;

    const todayIso = this.todayIso();
    this.stats.appointmentsToday = list.filter(a => a.dateIso === todayIso).length;

    const [ws, we] = this.weekRange(new Date());
    this.stats.appointmentsThisWeek = list.filter(a => {
      const s = a.slotStartDateTime ? new Date(a.slotStartDateTime) : null;
      if (!s || Number.isNaN(s.getTime())) return false;
      return s >= ws && s <= we;
    }).length;
  }

  private buildToday(list: UiAppt[]) {
    const todayIso = this.todayIso();
    this.todayAppointments = list
      .filter(a => a.dateIso === todayIso)
      .sort((a, b) => (a.slotStartDateTime || '').localeCompare(b.slotStartDateTime || ''));
  }

  private buildNext(list: UiAppt[]) {
    const now = new Date();
    const future = list
      .filter(a => {
        const s = a.slotStartDateTime ? new Date(a.slotStartDateTime) : null;
        return !!s && !Number.isNaN(s.getTime()) && s >= now && !this.eq(a.status, 'CANCELED');
      })
      .sort((a, b) => (a.slotStartDateTime || '').localeCompare(b.slotStartDateTime || ''));

    this.nextAppointment = future.length ? future[0] : null;
  }

  // -----------------------
  // UI mapping
  // -----------------------
  private toUi(a: Appointment): UiAppt {
    const startIso = a.slotStartDateTime || '';
    const endIso = a.slotEndDateTime || '';

    const dateIso = startIso.slice(0, 10);
    const timeLabel = this.timeRangeLabel(startIso, endIso);

    const st = ((a.status || 'CONFIRMED') as any).toString().toUpperCase() as AppointmentStatus;

    const patientLabel = (a as any).patientName
      ? String((a as any).patientName)
      : (a.patientId ? `Patient #${a.patientId}` : 'Patient');

    const canAction = this.eq(st, 'CONFIRMED') || this.eq(st, 'BOOKED');
    const canCancel = canAction;
    const canComplete = canAction;
    const canNoShow = canAction;

    return {
      ...a,
      dateIso,
      timeLabel,
      statusClass: this.statusClass(st),
      patientLabel,
      canCancel,
      canComplete,
      canNoShow
    };
  }

  private statusClass(st: AppointmentStatus): string {
    const s = (st || '').toString().toUpperCase();
    return s === 'CONFIRMED' ? 'bg-blue-50 text-blue-700 border-blue-200'
      : s === 'BOOKED' ? 'bg-amber-50 text-amber-700 border-amber-200'
      : s === 'COMPLETED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
      : s === 'CANCELED' ? 'bg-rose-50 text-rose-700 border-rose-200'
      : s === 'NO_SHOW' ? 'bg-slate-50 text-slate-700 border-slate-200'
      : 'bg-slate-50 text-slate-700 border-slate-200';
  }

  private timeRangeLabel(startIso?: string, endIso?: string): string {
    const s = startIso ? startIso.slice(11, 16) : '—';
    const e = endIso ? endIso.slice(11, 16) : '';
    return e ? `${s} — ${e}` : s;
  }

  private eq(st: any, expected: AppointmentStatus): boolean {
    return (st || '').toString().toUpperCase() === expected;
  }

  private todayIso(): string {
    return this.toDateIso(new Date());
  }

  private toDateIso(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }

  private weekRange(now: Date): [Date, Date] {
    // Monday -> Sunday
    const d = new Date(now);
    const day = d.getDay(); // 0 Sun ... 6 Sat
    const diffToMon = (day === 0 ? -6 : 1 - day);

    const start = new Date(d);
    start.setDate(d.getDate() + diffToMon);
    start.setHours(0, 0, 0, 0);

    const end = new Date(start);
    end.setDate(start.getDate() + 6);
    end.setHours(23, 59, 59, 999);

    return [start, end];
  }
}
