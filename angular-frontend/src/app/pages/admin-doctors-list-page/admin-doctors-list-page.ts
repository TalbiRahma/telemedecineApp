import { Component, HostListener, OnInit } from '@angular/core';
import { NgFor, NgIf, NgClass, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Doctor } from '../../models/doctor';
import { DoctorService } from '../../services/doctor/doctor-service';

type DoctorState = 'PENDING' | 'CONFIRMED' | 'REJECTED';
type StateFilter = 'ALL' | DoctorState;

@Component({
  selector: 'app-admin-doctors-list-page',
  standalone: true,
  imports: [NgFor, NgIf, NgClass, TitleCasePipe, FormsModule],
  templateUrl: './admin-doctors-list-page.html',
})
export class AdminDoctorsListPage implements OnInit {

  // -------------------------
  // Data
  // -------------------------
  doctors: Doctor[] = [];
  loading = false;
  errorMsg = '';

  // -------------------------
  // Query UI
  // -------------------------
  searchTerm = '';
  stateFilter: StateFilter = 'ALL';

  // -------------------------
  // Pagination
  // -------------------------
  currentPage = 1;
  pageSize = 10;

  // -------------------------
  // Derived lists
  // -------------------------
  filteredDoctors: Doctor[] = [];
  pagedDoctors: Doctor[] = [];

  // -------------------------
  // Modal
  // -------------------------
  isModalOpen = false;
  selectedDoctor: Doctor | null = null;
  pendingAction: 'confirm' | 'reject' | null = null;
  actionLoading = false;
  toastMsg = '';

  constructor(private doctorService: DoctorService) {}

  ngOnInit(): void {
    this.loadDoctors();
  }

  // -------------------------
  // Load from backend
  // -------------------------
  loadDoctors() {
    this.loading = true;
    this.errorMsg = '';

    this.doctorService.getAllDoctors().subscribe({
      next: (data) => {
        this.doctors = data ?? [];
        this.loading = false;
        this.applyQuery(); // ✅ refresh filtered + paged
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Unable to load doctors.';
        this.loading = false;

        // still keep derived lists consistent
        this.doctors = [];
        this.applyQuery();
      }
    });
  }

  // -------------------------
  // Search / Filter / Pagination
  // -------------------------
  onSearchChange() {
    this.currentPage = 1;
    this.applyQuery();
  }

  onPageSizeChange() {
    this.currentPage = 1;
    this.applyQuery();
  }

  resetQuery() {
    this.searchTerm = '';
    this.stateFilter = 'ALL';
    this.pageSize = 10;
    this.currentPage = 1;
    this.applyQuery();
  }

  applyQuery() {
    const term = (this.searchTerm || '').trim().toLowerCase();

    let list = [...this.doctors];

    // 1) State filter
    if (this.stateFilter !== 'ALL') {
      list = list.filter(d => (d.state as DoctorState) === this.stateFilter);
    }

    // 2) Search by name/email/specialty
    if (term) {
      list = list.filter(d => {
        const fullName = `${d.firstname || ''} ${d.lastname || ''}`.toLowerCase();
        const email = (d.email || '').toLowerCase();
        const specialty = (d.specialty?.name || '').toLowerCase();
        const license = String((d as any).licenseNumber ?? '').toLowerCase();
        return (
          fullName.includes(term) ||
          email.includes(term) ||
          specialty.includes(term) ||
          license.includes(term)
        );
      });
    }

    this.filteredDoctors = list;

    // 3) Clamp page
    const tp = this.totalPages;
    this.currentPage = Math.min(Math.max(this.currentPage, 1), tp);

    // 4) Slice
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.pagedDoctors = this.filteredDoctors.slice(start, end);
  }

  goToPage(page: number) {
    const tp = this.totalPages;
    const next = Math.min(Math.max(page, 1), tp);
    if (next === this.currentPage) return;
    this.currentPage = next;
    this.applyQuery();
  }

  // -------------------------
  // Pagination helpers (template)
  // -------------------------
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredDoctors.length / this.pageSize));
  }

  get rangeStart(): number {
    if (this.filteredDoctors.length === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get rangeEnd(): number {
    if (this.filteredDoctors.length === 0) return 0;
    return Math.min(this.currentPage * this.pageSize, this.filteredDoctors.length);
  }

  // show max 5 pages around current
  get visiblePages(): number[] {
    const tp = this.totalPages;
    const windowSize = 5;

    let start = Math.max(1, this.currentPage - 2);
    let end = Math.min(tp, start + windowSize - 1);

    start = Math.max(1, end - windowSize + 1);

    const pages: number[] = [];
    for (let p = start; p <= end; p++) pages.push(p);
    return pages;
  }

  // -------------------------
  // Modal
  // -------------------------
  openModal(doctor: Doctor) {
    this.doctorService.getDoctorById(doctor.id).subscribe({
      next: (full) => {
        this.selectedDoctor = full;
        this.isModalOpen = true;
      },
      error: (err) => {
        console.error(err);
        // fallback: show basic doctor
        this.selectedDoctor = doctor;
        this.isModalOpen = true;
      }
    });
  }

  closeModal() {
    if (this.actionLoading) return;
    this.isModalOpen = false;
    this.selectedDoctor = null;
    this.pendingAction = null;
  }

  get pendingCount(): number { return this.doctors.filter(d => d.state === 'PENDING').length; }

  @HostListener('document:keydown.escape') onEscape() { if (this.pendingAction) this.pendingAction = null; else if (this.isModalOpen) this.closeModal(); }
  initials(doctor: Doctor): string { return `${doctor.firstname?.[0] || ''}${doctor.lastname?.[0] || ''}`.toUpperCase() || 'DR'; }

  // -------------------------
  // Actions: Confirm / Reject
  // -------------------------
  confirmDoctor() {
    if (!this.selectedDoctor) return;
    this.actionLoading = true;
    this.doctorService
      .updateDoctor(this.selectedDoctor.id, { state: 'CONFIRMED' })
      .subscribe({
        next: (updated) => {
          this.doctors = this.doctors.map(d => d.id === updated.id ? updated : d);

          // keep modal updated
          this.selectedDoctor = updated;

          // refresh table (search/filter/pagination)
          this.applyQuery();

          this.actionLoading = false;
          this.pendingAction = null;
          this.toastMsg = 'Doctor confirmed successfully.';
          this.closeModal();
        },
        error: (err) => { console.error(err); this.actionLoading = false; this.errorMsg = 'Unable to confirm doctor.'; this.pendingAction = null; }
      });
  }

  rejectDoctor() {
    if (!this.selectedDoctor) return;
    this.actionLoading = true;
    this.doctorService
      .updateDoctor(this.selectedDoctor.id, { state: 'REJECTED' })
      .subscribe({
        next: (updated) => {
          this.doctors = this.doctors.map(d => d.id === updated.id ? updated : d);

          // keep modal updated
          this.selectedDoctor = updated;

          // refresh table (search/filter/pagination)
          this.applyQuery();

          this.actionLoading = false;
          this.pendingAction = null;
          this.toastMsg = 'Doctor application rejected.';
          this.closeModal();
        },
        error: (err) => { console.error(err); this.actionLoading = false; this.errorMsg = 'Unable to reject doctor.'; this.pendingAction = null; }
      });
  }
}
