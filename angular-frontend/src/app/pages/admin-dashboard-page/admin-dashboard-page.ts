import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { Authentication } from '../../services/auth/authentication';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-dashboard-page.html',
  styleUrls: ['./admin-dashboard-page.scss', './admin-specialties-nav.scss']
})
export class AdminDashboardPage implements OnInit, OnDestroy {
  usersOpen = true;
  currentPageTitle = 'Dashboard Overview';
  sidebarOpen = false;
  adminName = 'Super Admin';
  private navigationSub?: Subscription;

  constructor(private router: Router, private route: ActivatedRoute, private authService: Authentication) {}

  ngOnInit(): void {
    this.updatePageTitle();
    this.navigationSub = this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => { this.updatePageTitle(); this.sidebarOpen = false; });
    const token = this.authService.getDecodedToken();
    const identity = token?.name || token?.fullName || token?.username || token?.sub;
    if (identity && typeof identity === 'string' && !identity.includes('@')) this.adminName = identity;
  }

  ngOnDestroy(): void { this.navigationSub?.unsubscribe(); }

  private updatePageTitle(): void {
    let active = this.route;
    while (active.firstChild) active = active.firstChild;
    this.currentPageTitle = active.snapshot.data['title'] || 'Dashboard Overview';
  }

  get adminInitials(): string {
    return this.adminName.split(/\s+/).filter(Boolean).slice(0, 2).map(value => value[0]).join('').toUpperCase() || 'SA';
  }

  toggleSidebar(): void { this.sidebarOpen = !this.sidebarOpen; }
  onLogout(): void { this.authService.logout(); }
}
