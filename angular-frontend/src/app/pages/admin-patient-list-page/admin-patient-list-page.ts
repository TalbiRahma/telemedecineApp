import { Component, HostListener, OnInit, inject } from '@angular/core';
import { NgFor, NgIf, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientDto } from '../../models/PatientDto';
import { PatientService } from '../../services/patient/patient-service';

type GenderFilter = 'ALL' | 'MALE' | 'FEMALE' | 'OTHER' | 'UNKNOWN';

@Component({
  selector: 'app-admin-patients-list-page',
  standalone: true,
  imports: [NgFor, NgIf, TitleCasePipe, FormsModule],
  templateUrl: './admin-patient-list-page.html',
})
export class AdminPatientsListPage implements OnInit {

  private patientService = inject(PatientService);

  // data
  patients: PatientDto[] = [];
  loading = false;
  errorMsg = '';

  // query UI
  searchTerm = '';
  genderFilter: GenderFilter = 'ALL';

  // pagination
  currentPage = 1;
  pageSize = 10;

  // derived
  filteredPatients: PatientDto[] = [];
  pagedPatients: PatientDto[] = [];

  // modal
  isModalOpen = false;
  selectedPatient: PatientDto | null = null;

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients() {
    this.loading = true;
    this.errorMsg = '';

    this.patientService.getAllPatients().subscribe({
      next: (data) => {
        this.patients = data ?? [];
        this.loading = false;
        this.applyQuery();
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Unable to load patients.';
        this.loading = false;
        this.patients = [];
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
    this.genderFilter = 'ALL';
    this.pageSize = 10;
    this.currentPage = 1;
    this.applyQuery();
  }

  applyQuery() {
    const term = (this.searchTerm || '').trim().toLowerCase();

    let list = [...this.patients];

    // gender filter
    if (this.genderFilter !== 'ALL') {
      if (this.genderFilter === 'UNKNOWN') {
        list = list.filter(p => !(p.gender || '').trim());
      } else {
        list = list.filter(p => (p.gender || '').toUpperCase() === this.genderFilter);
      }
    }

    // search by name/email/phone/id
    if (term) {
      list = list.filter(p => {
        const fullName = `${p.firstname || ''} ${p.lastname || ''}`.toLowerCase();
        const email = (p.email || '').toLowerCase();
        const phone = (p.phone || '').toLowerCase();
        const id = String(p.id ?? '').toLowerCase();
        return fullName.includes(term) || email.includes(term) || phone.includes(term) || id.includes(term);
      });
    }

    this.filteredPatients = list;

    // clamp page
    const tp = this.totalPages;
    this.currentPage = Math.min(Math.max(this.currentPage, 1), tp);

    // slice
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.pagedPatients = this.filteredPatients.slice(start, end);
  }

  goToPage(page: number) {
    const tp = this.totalPages;
    const next = Math.min(Math.max(page, 1), tp);
    if (next === this.currentPage) return;
    this.currentPage = next;
    this.applyQuery();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredPatients.length / this.pageSize));
  }

  get rangeStart(): number {
    if (this.filteredPatients.length === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get rangeEnd(): number {
    if (this.filteredPatients.length === 0) return 0;
    return Math.min(this.currentPage * this.pageSize, this.filteredPatients.length);
  }

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
  openModal(patient: PatientDto) {
    // optional: fetch full data by id
    if (!patient?.id) {
      this.selectedPatient = patient;
      this.isModalOpen = true;
      return;
    }

    this.patientService.getById(patient.id).subscribe({
      next: (full) => {
        this.selectedPatient = full;
        this.isModalOpen = true;
      },
      error: (err) => {
        console.error(err);
        this.selectedPatient = patient;
        this.isModalOpen = true;
      }
    });
  }

  closeModal() {
    this.isModalOpen = false;
    this.selectedPatient = null;
  }
  @HostListener('document:keydown.escape') onEscape() { if (this.isModalOpen) this.closeModal(); }
  initials(patient: PatientDto): string { return `${patient.firstname?.[0] || ''}${patient.lastname?.[0] || ''}`.toUpperCase() || 'PT'; }
}
