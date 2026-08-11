import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { Authentication } from '../../services/auth/authentication';
import { CommonModule } from '@angular/common';
import { DoctorService } from '../../services/doctor/doctor-service';

type NavItem = {
  label: string;
  path: string;
  icon?: string;      // material-icons-outlined name
  title: string;      // page title
};

@Component({
  selector: 'app-doctor-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './doctor-dashboard-page.html',
  styleUrl: './doctor-dashboard-page.scss'
})
export class DoctorDashboardPage implements OnInit, OnDestroy {

  // ✅ navbar dynamic
  navItems: NavItem[] = [
    { label: 'Dashboard',    path: '/doctor-dashboard/overview',     icon: 'dashboard',   title: 'Dashboard' },
    { label: 'Appointments', path: '/doctor-dashboard/appointments', icon: 'event',       title: 'My Appointments' },
    { label: 'Availability', path: '/doctor-dashboard/availability', icon: 'schedule',    title: 'My Availability' },
    { label: 'Profile', path: '/doctor-dashboard/profile', icon: 'person', title: 'My Profile' }
  ];

  currentPageTitle = 'Dashboard';
  doctorName = 'Doctor';
  doctorSpecialty = 'Doctor account';
  menuOpen = false;

  private sub?: Subscription;

  constructor(
    private router: Router,
    private authService: Authentication,
    private doctorService: DoctorService
  ) {}

  ngOnInit(): void {
    // set initial title
    this.setTitleFromUrl(this.router.url);

    // update title on navigation
    this.sub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => {
        this.setTitleFromUrl(e.urlAfterRedirects || e.url);
        this.menuOpen = false;
      });

    const doctorId = this.authService.getUserId();
    if (doctorId) {
      this.doctorService.getDoctorById(doctorId).subscribe({
        next: doctor => {
          this.doctorName = `${doctor.firstname || ''} ${doctor.lastname || ''}`.trim() || 'Doctor';
          this.doctorSpecialty = doctor.specialty?.name || 'Doctor account';
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  // ✅ title auto from nav config
  private setTitleFromUrl(url: string) {
    const found = this.navItems.find(i => url.startsWith(i.path));
    this.currentPageTitle = found?.title || 'Doctor Dashboard';
  }

  // ✅ optional: show doctor name if you store it in auth/localStorage
  get doctorDisplayName(): string {
    // عدّل حسب auth متاعك (هاذي safe fallback)
    return this.doctorName === 'Doctor' ? 'Doctor' : `Dr. ${this.doctorName}`;
  }

  get doctorInitials(): string {
    return this.doctorName.split(' ').filter(Boolean).slice(0, 2)
      .map(part => part.charAt(0)).join('').toUpperCase() || 'DR';
  }

  toggleMenu(): void { this.menuOpen = !this.menuOpen; }

  onLogout() {
    this.authService.logout();
  }
}
