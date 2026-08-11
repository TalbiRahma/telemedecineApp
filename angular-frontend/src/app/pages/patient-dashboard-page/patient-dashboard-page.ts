import { CommonModule } from '@angular/common';
import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Authentication } from '../../services/auth/authentication';
import { PatientService } from '../../services/patient/patient-service';

@Component({
  selector: 'app-patient-dashboard-page',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './patient-dashboard-page.html',
  styleUrl: './patient-dashboard-page.scss',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class PatientDashboardPage implements OnInit {
  currentPageTitle = 'Dashboard';
  patientName = 'Patient';
  menuOpen = false;

  constructor(private router: Router, private authService: Authentication, private patientService: PatientService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.updatePageTitle(event.url);
        this.menuOpen = false;
      }
    });
  }

  ngOnInit(): void {
    this.patientService.getMe().subscribe({
      next: patient => this.patientName = `${patient.firstname ?? ''} ${patient.lastname ?? ''}`.trim() || 'Patient',
      error: () => this.patientName = 'Patient'
    });
  }

  updatePageTitle(url: string): void {
    if (url.includes('/appointments')) this.currentPageTitle = 'My appointments';
    else if (url.includes('/find-doctor')) this.currentPageTitle = 'Find a doctor';
    else if (url.includes('/profile')) this.currentPageTitle = 'My profile';
    else if (url.includes('/symptoms-checker')) this.currentPageTitle = 'Symptom checker';
    else this.currentPageTitle = 'Dashboard';
  }

  getPageTitle(): string { return this.currentPageTitle; }
  get initials(): string { return this.patientName.split(' ').filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase() || 'P'; }
  toggleMenu(): void { this.menuOpen = !this.menuOpen; }
  onLogout(): void { this.authService.logout(); }
}
