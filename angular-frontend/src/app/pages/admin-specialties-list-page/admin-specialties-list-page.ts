import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf, NgFor } from '@angular/common';
import { Specialty } from '../../models/specialty';
import { SpecialtyService } from '../../services/specialty/specialty-service';

type FilterMode = 'all' | 'withDesc' | 'withoutDesc';

@Component({
  selector: 'app-admin-specialties-list-page',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor],
  templateUrl: './admin-specialties-list-page.html',
})
export class AdminSpecialtiesListPage implements OnInit {

  // -------------------------
  // Data
  // -------------------------
  specialties: Specialty[] = [];
  loading = false;
  saving = false;
  errorMsg = '';
  toastMsg = '';

  // -------------------------
  // Query UI
  // -------------------------
  searchTerm = '';
  filterMode: FilterMode = 'all';

  // -------------------------
  // Pagination
  // -------------------------
  currentPage = 1;
  pageSize = 10;

  // -------------------------
  // Derived lists
  // -------------------------
  filteredSpecialties: Specialty[] = [];
  pagedSpecialties: Specialty[] = [];

  // -------------------------
  // Modal state
  // -------------------------
  isModalOpen = false;
  editingSpecialty: Specialty | null = null;
  deletingSpecialty: Specialty | null = null;

  specialtyForm: Partial<Specialty> = {
    name: '',
    description: ''
  };

  constructor(private specialtyService: SpecialtyService) {}

  ngOnInit(): void {
    this.loadSpecialties();
  }

  // -------------------------
  // Backend load
  // -------------------------
  loadSpecialties() {
    this.loading = true;
    this.errorMsg = '';
    this.specialtyService.getAllSpecialties().subscribe({
      next: (data) => {
        this.specialties = data ?? [];
        this.loading = false;
        this.applyQuery(); // refresh filtered + paged
      },
      error: (err) => { console.error('Error loading specialties', err); this.loading = false; this.errorMsg = 'Unable to load specialties.'; }
    });
  }

  // -------------------------
  // Search / Filter / Pagination logic
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
    this.filterMode = 'all';
    this.pageSize = 10;
    this.currentPage = 1;
    this.applyQuery();
  }

  applyQuery() {
    const term = (this.searchTerm || '').trim().toLowerCase();

    // 1) Base list
    let list = [...this.specialties];

    // 2) Filter (description presence)
    if (this.filterMode === 'withDesc') {
      list = list.filter(s => !!(s.description || '').trim());
    } else if (this.filterMode === 'withoutDesc') {
      list = list.filter(s => !(s.description || '').trim());
    }

    // 3) Search (name/description/id)
    if (term) {
      list = list.filter(s => {
        const name = (s.name || '').toLowerCase();
        const desc = (s.description || '').toLowerCase();
        const id = String(s.id ?? '').toLowerCase();
        return name.includes(term) || desc.includes(term) || id.includes(term);
      });
    }

    this.filteredSpecialties = list;

    // 4) Clamp current page
    const tp = this.totalPages;
    this.currentPage = Math.min(Math.max(this.currentPage, 1), tp);

    // 5) Slice for pagination
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.pagedSpecialties = this.filteredSpecialties.slice(start, end);
  }

  goToPage(page: number) {
    const tp = this.totalPages;
    const next = Math.min(Math.max(page, 1), tp);
    if (next === this.currentPage) return;
    this.currentPage = next;
    this.applyQuery();
  }

  // -------------------------
  // Pagination helpers (used in template)
  // -------------------------
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredSpecialties.length / this.pageSize));
  }

  get rangeStart(): number {
    if (this.filteredSpecialties.length === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get rangeEnd(): number {
    if (this.filteredSpecialties.length === 0) return 0;
    return Math.min(this.currentPage * this.pageSize, this.filteredSpecialties.length);
  }

  // shows max 5 pages around current
  get visiblePages(): number[] {
    const tp = this.totalPages;
    const windowSize = 5;

    let start = Math.max(1, this.currentPage - 2);
    let end = Math.min(tp, start + windowSize - 1);

    // if we're near the end, shift window back
    start = Math.max(1, end - windowSize + 1);

    const pages: number[] = [];
    for (let p = start; p <= end; p++) pages.push(p);
    return pages;
  }

  // -------------------------
  // Modal actions
  // -------------------------
  openAddModal() {
    this.editingSpecialty = null;
    this.specialtyForm = { name: '', description: '' };
    this.isModalOpen = true;
  }

  openEditModal(specialty: Specialty) {
    this.editingSpecialty = specialty;
    this.specialtyForm = { ...specialty };
    this.isModalOpen = true;
  }

  closeModal() {
    if (this.saving) return;
    this.isModalOpen = false;
  }
  @HostListener('document:keydown.escape') onEscape() { if (this.deletingSpecialty) this.deletingSpecialty = null; else if (this.isModalOpen) this.closeModal(); }

  // -------------------------
  // CRUD
  // -------------------------
  saveSpecialty() {
    if (!this.specialtyForm.name?.trim()) return;
    this.saving = true;
    this.errorMsg = '';

    if (this.editingSpecialty) {
      // UPDATE
      this.specialtyService
        .update(this.editingSpecialty.id, this.specialtyForm)
        .subscribe({
          next: () => {
            this.saving = false;
            this.toastMsg = 'Specialty updated.';
            this.loadSpecialties();
            this.closeModal();
          },
          error: (err) => { console.error('Error updating specialty', err); this.saving = false; this.errorMsg = 'Unable to update specialty.'; }
        });
    } else {
      // CREATE
      this.specialtyService
        .add(this.specialtyForm)
        .subscribe({
          next: () => {
            this.saving = false;
            this.toastMsg = 'Specialty created.';
            this.loadSpecialties();
            this.closeModal();
          },
          error: (err) => { console.error('Error adding specialty', err); this.saving = false; this.errorMsg = 'Unable to create specialty.'; }
        });
    }
  }

  deleteSpecialty() {
    if (!this.deletingSpecialty) return;
    const specialty = this.deletingSpecialty;
    this.saving = true;
    this.specialtyService.delete(specialty.id).subscribe({
      next: () => { this.saving = false; this.deletingSpecialty = null; this.toastMsg = 'Specialty deleted.'; this.loadSpecialties(); },
      error: (err) => { console.error('Error deleting specialty', err); this.saving = false; this.deletingSpecialty = null; this.errorMsg = err?.error?.message || err?.error || 'Unable to delete specialty. It may be in use by a doctor.'; }
    });
  }
  descriptionText(specialty: Specialty): string { return specialty.description?.trim() || 'No description provided.'; }
}
