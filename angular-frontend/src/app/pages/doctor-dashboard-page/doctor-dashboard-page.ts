import { Component } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet } from '@angular/router';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-doctor-dashboard-page',
  imports: [RouterOutlet],
  templateUrl: './doctor-dashboard-page.html',
  styleUrl: './doctor-dashboard-page.scss'
})
export class DoctorDashboardPage {

   currentPageTitle = 'My Patients';

  constructor(private router: Router, private authService: Authentication) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.updatePageTitle(event.url);
      }
    });
  }

  updatePageTitle(url: string) {
    if (url.includes('/patients')) this.currentPageTitle = 'My Patients';
    else if (url.includes('/appointments')) this.currentPageTitle = 'My Appointments';
    else if (url.includes('/profile')) this.currentPageTitle = 'Profile';
  }

  getPageTitle(): string {
    return this.currentPageTitle;
  }

  onLogout() {
    this.authService.logout();
  }

}
