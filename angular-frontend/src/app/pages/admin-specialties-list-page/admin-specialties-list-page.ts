import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf, NgFor } from '@angular/common';
import { Specialty } from '../../models/specialty';
import { SpecialtyService } from '../../services/specialty/specialty-service';

@Component({
  selector: 'app-admin-specialties-list-page',
  imports: [FormsModule, NgIf, NgFor],
  templateUrl: './admin-specialties-list-page.html',
  styleUrl: './admin-specialties-list-page.scss'
})
export class AdminSpecialtiesListPage implements OnInit {

  specialties: Specialty[] = [];
  isModalOpen = false;
  editingSpecialty: Specialty | null = null;
  specialtyForm: Partial<Specialty> = {};

   constructor(private specialtyService: SpecialtyService) {}

  ngOnInit(): void {
    this.loadSpecialties();
  }

  loadSpecialties() {
    this.specialtyService.getAllSpecialties().subscribe({
      next: (data) => this.specialties = data,
      error: (err) => console.error('Failed to load specialties:', err)
    });
  }

  openAddModal() {
    this.editingSpecialty = null;
    this.specialtyForm = {};
    this.isModalOpen = true;
  }

  openEditModal(specialty: Specialty) {
    this.editingSpecialty = specialty;
    this.specialtyForm = { ...specialty };
    this.isModalOpen = true;
  }

  saveSpecialty() {
    if (this.editingSpecialty) {
      // Update existing
      this.specialtyService.update(this.editingSpecialty.id, this.specialtyForm).subscribe({
        next: () => this.loadSpecialties()
      });
    } else {
      // Add new
      this.specialtyService.add(this.specialtyForm).subscribe({
        next: () => this.loadSpecialties()
      });
    }
    this.closeModal();
  }

    deleteSpecialty(id: number) {
    if (confirm('Are you sure you want to delete this specialty?')) {
      this.specialtyService.delete(id).subscribe({
        next: () => this.loadSpecialties(),
        error: (err) => console.error('Delete failed:', err)
      });
    }
  }

  closeModal() {
    this.isModalOpen = false;
  }

}
