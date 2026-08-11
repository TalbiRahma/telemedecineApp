// ✅ patient-appointments-page.ts (FULL updated file)
// IMPORTANT: I only added "doctorPhone" to search + UI type safety.
// Everything else is the same as your code.

import { CommonModule, NgClass } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Appointment } from '../../models/Appointment ';
import { AppointmentService } from '../../services/appointement/appointment-service';
import { FormsModule } from '@angular/forms';

type UiAppointment = Appointment & {
  dateLabel: string;
  timeLabel: string;
  statusLabel: string;
  statusClass: string;
};

type StatusFilter = 'ALL' | 'BOOKED' | 'CONFIRMED' | 'CANCELED' | 'COMPLETED' | 'NO_SHOW';

@Component({
  selector: 'app-patient-appointments-page',
  standalone: true,
  imports: [CommonModule, RouterLink, NgClass, FormsModule],
  templateUrl: './patient-appointments-page.html',
  styleUrl: './patient-appointments-page.scss'
})
export class PatientAppointmentsPage implements OnInit {

  loading = false;
  errorMsg = '';
  successMsg = '';

  // ✅ raw list
  appointments: UiAppointment[] = [];

  // ✅ filtered list
  filtered: UiAppointment[] = [];

  // ✅ pagination list
  paged: UiAppointment[] = [];

  // -----------------------
  // Filters + Search
  // -----------------------
  query = '';
  statusFilter: StatusFilter = 'ALL';
  fromDate = ''; // YYYY-MM-DD
  toDate = '';   // YYYY-MM-DD

  // -----------------------
  // Pagination
  // -----------------------
  page = 1;
  pageSize = 6;
  totalPages = 1;

  constructor(
    private appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments() {
    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.appointmentService.getMine().subscribe({
      next: (data) => {
        const list = (data || []).slice();

        // sort newest first by bookedAt if exists
        list.sort((a, b) => (b.bookedAt || '').localeCompare(a.bookedAt || ''));

        this.appointments = list.map(a => this.toUi(a));

        // ✅ apply filters + pagination
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Failed to load appointments.';
        this.loading = false;
      }
    });
  }

  cancelAppointment(appt: UiAppointment) {
    if (!appt.id) return;

    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.appointmentService.cancel(appt.id).subscribe({
      next: () => {
        // update locally
        this.appointments = this.appointments.map(a =>
          a.id === appt.id ? this.toUi({ ...a, status: 'CANCELED' as any }) : a
        );

        this.successMsg = 'Appointment canceled.';
        this.loading = false;

        // ✅ re-apply filters and pagination (in case list changes)
        this.applyFilters(false); // keep page if possible
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Failed to cancel appointment.';
        this.loading = false;
      }
    });
  }

  // -----------------------
  // Filters + Search + Pagination
  // -----------------------
  onSearchEnter() {
    this.applyFilters(true);
  }

  onFiltersChange() {
    this.applyFilters(true);
  }

  clearFilters() {
    this.query = '';
    this.statusFilter = 'ALL';
    this.fromDate = '';
    this.toDate = '';
    this.applyFilters(true);
  }

  applyFilters(resetPage: boolean = true) {
    const q = (this.query || '').trim().toLowerCase();
    const st = this.statusFilter;

    const fromTs = this.fromDate ? new Date(this.fromDate + 'T00:00:00').getTime() : null;
    const toTs = this.toDate ? new Date(this.toDate + 'T23:59:59').getTime() : null;

    this.filtered = this.appointments.filter(a => {
      const status = (a.status || '').toString().toUpperCase();

      // status filter
      const matchesStatus = (st === 'ALL') || status === st;

      // date filter (based on slotStartDateTime)
      const slotTs = a.slotStartDateTime ? new Date(a.slotStartDateTime).getTime() : null;
      const matchesFrom = !fromTs || (slotTs != null && slotTs >= fromTs);
      const matchesTo = !toTs || (slotTs != null && slotTs <= toTs);

      // search: id / doctor name / specialty / address / ✅ phone
      const idStr = (a.id ?? '').toString();
      const docName = `${a.doctorFirstname ?? ''} ${a.doctorLastname ?? ''}`.trim().toLowerCase();
      const spec = (a.doctorSpecialtyName ?? '').trim().toLowerCase();
      const addr = (a.doctorAdresse ?? '').trim().toLowerCase();
      const phone = (a.doctorPhone ?? '').trim().toLowerCase(); // ✅ added

      const matchesQuery =
        !q ||
        idStr.includes(q) ||
        docName.includes(q) ||
        spec.includes(q) ||
        addr.includes(q) ||    // ✅ already
        phone.includes(q);     // ✅ added

      return matchesStatus && matchesFrom && matchesTo && matchesQuery;
    });

    // ✅ reset or keep page
    if (resetPage) this.page = 1;

    this.updatePagination();
  }

  updatePagination() {
    const total = this.filtered.length;
    this.totalPages = Math.max(1, Math.ceil(total / this.pageSize));

    if (this.page > this.totalPages) this.page = this.totalPages;
    if (this.page < 1) this.page = 1;

    const start = (this.page - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.paged = this.filtered.slice(start, end);
  }

  onPageSizeChange() {
    this.page = 1;
    this.updatePagination();
  }

  goToPage(p: number) {
    this.page = p;
    this.updatePagination();
  }

  prevPage() {
    if (this.page > 1) {
      this.page--;
      this.updatePagination();
    }
  }

  nextPage() {
    if (this.page < this.totalPages) {
      this.page++;
      this.updatePagination();
    }
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  // -----------------------
  // UI helpers
  // -----------------------
  private toUi(a: Appointment): UiAppointment {
    const dt = a.slotStartDateTime ? new Date(a.slotStartDateTime) : null;

    const dateLabel = dt && !Number.isNaN(dt.getTime())
      ? dt.toLocaleDateString(undefined, { weekday: 'short', year: 'numeric', month: 'short', day: '2-digit' })
      : '—';

    const timeLabel = dt && !Number.isNaN(dt.getTime())
      ? dt.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
      : '—';

    const status = (a.status || '').toString().toUpperCase();

    const statusLabel =
      status === 'BOOKED' ? 'Booked' :
      status === 'CONFIRMED' ? 'Confirmed' :
      status === 'CANCELED' ? 'Canceled' :
      status === 'COMPLETED' ? 'Completed' :
      status === 'NO_SHOW' ? 'No show' :
      status || '—';

    const statusClass =
      status === 'BOOKED' ? 'bg-amber-50 text-amber-700 border-amber-200' :
      status === 'CONFIRMED' ? 'bg-blue-50 text-blue-700 border-blue-200' :
      status === 'CANCELED' ? 'bg-rose-50 text-rose-700 border-rose-200' :
      status === 'COMPLETED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
      status === 'NO_SHOW' ? 'bg-slate-100 text-slate-700 border-slate-200' :
      'bg-slate-50 text-slate-700 border-slate-200';

    return { ...a, dateLabel, timeLabel, statusLabel, statusClass };
  }

  canCancel(a: UiAppointment): boolean {
    const st = (a.status || '').toString().toUpperCase();
    return st === 'BOOKED' || st === 'CONFIRMED';
  }

  doctorFullName(a: Appointment): string {
    const fn = (a.doctorFirstname || '').trim();
    const ln = (a.doctorLastname || '').trim();
    const full = `${fn} ${ln}`.trim();
    return full ? `Dr. ${full}` : 'Doctor —';
  }
}
