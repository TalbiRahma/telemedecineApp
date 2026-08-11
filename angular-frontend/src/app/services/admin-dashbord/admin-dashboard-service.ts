import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdminOverviewStatsDto {
  totalPatients: number;
  totalDoctors: number;
  pendingDoctors: number;
  confirmedDoctors: number;
  rejectedDoctors: number;
  totalAppointments: number;
  totalUsers: number;
}

@Injectable({ providedIn: 'root' })
export class AdminDashboardService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/admin/dashboard';

  getOverviewStats(): Observable<AdminOverviewStatsDto> {
    return this.http.get<AdminOverviewStatsDto>(`${this.baseUrl}/overview`);
  }
}
