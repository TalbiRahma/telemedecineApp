import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Appointment } from '../../models/Appointment ';
import { Slot } from '../../models/slot';
import { HttpParams } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private baseUrl = '/api/appointments';

  constructor(private http: HttpClient) {}

  book(dto: Pick<Appointment, 'slotId' | 'slotStartDateTime'>): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.baseUrl}/book`, dto);
  }

  getAppointment(id: number): Observable<Appointment> {
    return this.http.get<Appointment>(`${this.baseUrl}/${id}`);
  }

  getByPatient(patientId: number): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.baseUrl}/patient/${patientId}`);
  }

  getBookableSlots(doctorId: number, from: string, to: string): Observable<Slot[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<Slot[]>(`${this.baseUrl}/doctors/${doctorId}/slots`, { params });
  }

  getMine(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.baseUrl}/patient/me`);
  }

  getByDoctor(doctorId: number): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.baseUrl}/doctor/${doctorId}`);
  }

  cancel(id: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}/cancel`, {});
  }

  complete(id: number): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.baseUrl}/${id}/complete`, {});
  }

  noShow(id: number): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.baseUrl}/${id}/noshow`, {});
  }
}
