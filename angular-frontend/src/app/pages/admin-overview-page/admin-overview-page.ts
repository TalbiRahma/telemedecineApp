import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, catchError, of, switchMap, takeUntil, timer } from 'rxjs';
import { AdminDashboardService, AdminOverviewStatsDto } from '../../services/admin-dashbord/admin-dashboard-service';

@Component({ selector: 'app-admin-overview-page', standalone: true, imports: [CommonModule, RouterLink], templateUrl: './admin-overview-page.html' })
export class AdminOverviewPage implements OnInit, OnDestroy {
  private api = inject(AdminDashboardService);
  private destroy$ = new Subject<void>();
  loading = true;
  errorMsg = '';
  stats: AdminOverviewStatsDto | null = null;

  ngOnInit(): void {
    timer(0, 15000).pipe(takeUntil(this.destroy$), switchMap(() => this.fetchStats())).subscribe(data => this.applyStats(data));
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  refreshNow(): void { this.loading = true; this.errorMsg = ''; this.fetchStats().subscribe(data => this.applyStats(data)); }
  private fetchStats() {
    return this.api.getOverviewStats().pipe(catchError(err => { console.error(err); this.errorMsg = 'Unable to load dashboard statistics.'; return of(null); }));
  }
  private applyStats(data: AdminOverviewStatsDto | null): void { this.loading = false; if (data) { this.stats = data; this.errorMsg = ''; } }
}
