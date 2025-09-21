import { Component } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { NgIf } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-admin-dashboard-page',
  imports: [NgIf, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-dashboard-page.html',
  styleUrl: './admin-dashboard-page.scss'
})
export class AdminDashboardPage {
  usersOpen = false;
  currentPageTitle = 'Dashboard Overview';

  constructor(
    private router: Router,
    private authService: Authentication
  ) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.updatePageTitle(event.url);
      }
    });
  }

  updatePageTitle(url: string) {
    if (url.includes('/users/doctors')) {
      this.currentPageTitle = 'Doctors Management';
    } else if (url.includes('/overview')) {
      this.currentPageTitle = 'Dashboard Overview';
    } else if (url.includes('/users/patients')) {
      this.currentPageTitle = 'Patients Management';
    } else if (url.includes('/reports')) {
      this.currentPageTitle = 'Reports';
    } else if (url.includes('/settings')) {
      this.currentPageTitle = 'Settings';
    }
  }

  getPageTitle(): string {
    return this.currentPageTitle;
  }

  onLogout() {
    this.authService.logout();
  }
}