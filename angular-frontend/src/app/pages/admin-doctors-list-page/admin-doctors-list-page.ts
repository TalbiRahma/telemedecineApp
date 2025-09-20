import { NgClass, NgFor, NgIf, TitleCasePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Doctor } from '../../models/doctor';
import { DoctorService } from '../../services/doctor/doctor-service';

@Component({
  selector: 'app-admin-doctors-list-page',
  imports: [NgFor, NgIf, TitleCasePipe, NgClass ],
  templateUrl: './admin-doctors-list-page.html',
  styleUrl: './admin-doctors-list-page.scss'
})
export class AdminDoctorsListPage implements OnInit{

  doctors: Doctor[] = [];
  selectedDoctor: Doctor | null = null;
  isModalOpen = false;

  
  constructor(private doctorService: DoctorService) {}

   ngOnInit(): void {
    this.loadDoctors();
  }

  loadDoctors() {
    this.doctorService.getAllDoctors().subscribe({
      next: (data) => {
        this.doctors = data;
      },
      error: (err) => {
        console.error('Error fetching doctors', err);
      }
    });
  }

  openModal(doctor: Doctor) {
    this.selectedDoctor = doctor;
    this.isModalOpen = true;
  }

  closeModal() {
    this.selectedDoctor = null;
    this.isModalOpen = false;
  }


  confirmDoctor() {
    if (this.selectedDoctor) {
      this.doctorService.updateDoctor(this.selectedDoctor.id!, { state: 'CONFIRMED' }).subscribe({
        next: (updated) => {
          // mettre à jour localement
          this.selectedDoctor!.state = updated.state;
          this.updateDoctorInList(updated);
          this.closeModal();
        },
        error: (err) => console.error('Error confirming doctor', err)
      });
    }
  }

  rejectDoctor() {
    if (this.selectedDoctor) {
      this.doctorService.updateDoctor(this.selectedDoctor.id!, { state: 'REJECTED' }).subscribe({
        next: (updated) => {
          this.selectedDoctor!.state = updated.state;
          this.updateDoctorInList(updated);
          this.closeModal();
        },
        error: (err) => console.error('Error rejecting doctor', err)
      });
    }
  }

  
  private updateDoctorInList(updated: Doctor) {
    const index = this.doctors.findIndex(d => d.id === updated.id);
    if (index !== -1) {
      this.doctors[index] = updated;
    }
  }

}

