import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Availability } from '../../models/availability';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DoctorAvailabilityService {
  private baseUrl = '/api/availabilities';

  constructor(private http: HttpClient) {}

  // rules saved in DB (ONE_OFF rules + RECURRING rules)
  getAllRules(): Observable<Availability[]> {
    return this.http.get<Availability[]>(`${this.baseUrl}/me/all`);
  }

  // expands RECURRING into dates
  getOccurrences(
    from: string, // YYYY-MM-DD
    to: string,   // YYYY-MM-DD
    includeSlots = false
  ): Observable<Availability[]> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('includeSlots', includeSlots);

    return this.http.get<Availability[]>(`${this.baseUrl}/me/occurrences`, { params });
  }

  add(dto: Availability): Observable<Availability> {
    return this.http.post<Availability>(`${this.baseUrl}/me`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/me/${id}`);
  }
}
