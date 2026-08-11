import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Slot, SlotStatus } from '../../models/slot';

@Injectable({ providedIn: 'root' })
export class SlotService {
  private baseUrl = '/api/slots';

  constructor(private http: HttpClient) {}

  getByAvailability(availabilityId: number): Observable<Slot[]> {
    return this.http.get<Slot[]>(`${this.baseUrl}/availability/${availabilityId}`);
  }

  generateSlots(availabilityId: number, durationMinutes: number): Observable<Slot[]> {
    const params = new HttpParams().set('durationMinutes', String(durationMinutes));

    return this.http.post<Slot[]>(
      `${this.baseUrl}/generate/${availabilityId}`,
      {},
      { params }
    );
  }

  updateStatus(slotId: number, status: SlotStatus): Observable<Slot> {
    return this.http.patch<Slot>(
      `${this.baseUrl}/${slotId}/status`,
      { status }
    );
  }

  // ✅ هذا لازم ل BookAppointmentPage
  getFreeSlotsByDoctor(doctorId: number, from: string, to: string): Observable<Slot[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<Slot[]>(`${this.baseUrl}/doctor/${doctorId}/free`, { params });
  }
}
