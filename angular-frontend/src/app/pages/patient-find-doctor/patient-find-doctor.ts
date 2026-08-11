// src/app/pages/patient-find-doctor/patient-find-doctor.ts
import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { DoctorService } from '../../services/doctor/doctor-service';
import { SpecialtyService } from '../../services/specialty/specialty-service';

import { Doctor } from '../../models/doctor';
import { Specialty } from '../../models/specialty';

@Component({
  selector: 'app-patient-find-doctor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './patient-find-doctor.html',
  styleUrl: './patient-find-doctor.scss'
})
export class PatientFindDoctor implements OnInit {

  // Pagination
  page = 1;
  pageSize = 6;
  totalPages = 1;
  pagedDoctors: Doctor[] = [];

  // data
  doctors: Doctor[] = [];
  filteredDoctors: Doctor[] = [];
  specialties: Specialty[] = [];

  // UI state
  loading = false;
  errorMsg = '';

  // filters
  query = '';
  selectedSpecialtyId: number | null = null;
  private requestedSpecialty = '';
  onlyTodayAvailable = false;

  // Modal
  profileOpen = false;
  selectedDoctor: Doctor | null = null;
  profileLoading = false;

  constructor(
    private doctorService: DoctorService,
    private specialtyService: SpecialtyService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.requestedSpecialty = this.route.snapshot.queryParamMap.get('specialty') ?? '';
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.errorMsg = '';

    forkJoin({
      specialties: this.specialtyService.getAllSpecialties(),
      doctors: this.doctorService.getBookableDoctors(),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ specialties, doctors }) => {
          this.specialties = specialties || [];
          this.doctors = doctors || [];
          this.preselectRequestedSpecialty();
          this.applyFilters();
        },
        error: (err) => {
          console.error(err);
          this.errorMsg = 'Failed to load data.';
        }
      });
  }

  private preselectRequestedSpecialty(): void {
    if (!this.requestedSpecialty) return;
    const requested = this.normalizeSpecialty(this.requestedSpecialty);
    const match = this.specialties.find((specialty) =>
      this.normalizeSpecialty(specialty.name ?? '') === requested
    );
    this.selectedSpecialtyId = match?.id ?? null;
  }

  private normalizeSpecialty(value: string): string {
    return value.trim().replace(/[\s-]+/g, '_').replace(/[^A-Za-z0-9_]/g, '').toUpperCase();
  }

  onSearchClick(): void {
    this.applyFilters();
  }

  onEnter(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.query = '';
    this.selectedSpecialtyId = null;
    this.onlyTodayAvailable = false;
    this.applyFilters();
  }

  toggleTodayAvailable(): void {
    this.onlyTodayAvailable = !this.onlyTodayAvailable;
    this.applyFilters();
  }

  applyFilters(): void {
    const q = (this.query || '').trim().toLowerCase();
    const specId = this.selectedSpecialtyId;

    this.filteredDoctors = this.doctors.filter((d) => {
      const fullName = `${d.firstname ?? ''} ${d.lastname ?? ''}`.trim().toLowerCase();
      const email = (d.email ?? '').trim().toLowerCase();
      const phone = (d.phone ?? '').trim().toLowerCase();           // âœ… search includes phone
      const address = (d.adresse ?? '').trim().toLowerCase();
      const specialtyName = (d.specialty?.name ?? '').trim().toLowerCase();

      const matchesQuery =
        !q ||
        fullName.includes(q) ||
        email.includes(q) ||
        phone.includes(q) ||                                      // âœ…
        specialtyName.includes(q) ||
        address.includes(q);

      const matchesSpecialty =
        specId == null || (d.specialty?.id ?? null) === specId;

      const matchesToday = !this.onlyTodayAvailable;

      return matchesQuery && matchesSpecialty && matchesToday;
    });

    this.page = 1;
    this.updatePagination();
  }

  // helpers
  trackByDoctorId(_: number, d: Doctor) {
    return d.id;
  }

  trackBySpecialtyId(_: number, s: Specialty) {
    return s.id;
  }

  getDoctorInitials(d: Doctor): string {
    const f = (d.firstname || '').charAt(0).toUpperCase();
    const l = (d.lastname || '').charAt(0).toUpperCase();
    return (f + l) || 'DR';
  }

  bookLink(d: Doctor) {
    return ['/patient-dashboard/book-appointment', d.id];
  }

  updatePagination(): void {
    const total = this.filteredDoctors.length;
    this.totalPages = Math.max(1, Math.ceil(total / this.pageSize));

    if (this.page > this.totalPages) this.page = this.totalPages;
    if (this.page < 1) this.page = 1;

    const start = (this.page - 1) * this.pageSize;
    const end = start + this.pageSize;

    this.pagedDoctors = this.filteredDoctors.slice(start, end);
  }

  goToPage(p: number): void {
    this.page = p;
    this.updatePagination();
  }

  nextPage(): void {
    if (this.page < this.totalPages) {
      this.page++;
      this.updatePagination();
    }
  }

  prevPage(): void {
    if (this.page > 1) {
      this.page--;
      this.updatePagination();
    }
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  onPageSizeChange(): void {
    this.page = 1;
    this.updatePagination();
  }

  // Modal open/close
  openProfile(d: Doctor): void {
    this.profileOpen = true;
    this.selectedDoctor = d;
  }

  closeProfile(): void {
    this.profileOpen = false;
    this.selectedDoctor = null;
    this.profileLoading = false;
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    if (this.profileOpen) this.closeProfile();
  }
}

