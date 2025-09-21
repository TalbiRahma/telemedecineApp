import { Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet } from '@angular/router';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-patient-dashboard-page',
  imports: [RouterOutlet],
  templateUrl: './patient-dashboard-page.html',
  styleUrl: './patient-dashboard-page.scss',
   schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class PatientDashboardPage {

  currentPageTitle = 'My Appointments';

  constructor(private router: Router, private authService: Authentication) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.updatePageTitle(event.url);
      }
    });
  }

   updatePageTitle(url: string) {
    if (url.includes('/appointments')) this.currentPageTitle = 'My Appointments';
    else if (url.includes('/prescriptions')) this.currentPageTitle = 'My Prescriptions';
    else if (url.includes('/profile')) this.currentPageTitle = 'Profile';
  }

  getPageTitle(): string {
    return this.currentPageTitle;
  }

  onLogout() {
    this.authService.logout();
  }
}
