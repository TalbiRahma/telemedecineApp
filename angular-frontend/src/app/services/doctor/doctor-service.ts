import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Doctor } from '../../models/doctor';

@Injectable({
  providedIn: 'root'
})
export class DoctorService {
  private baseUrl = 'http://localhost:8080/api/v1/doctor';
   constructor(private http: HttpClient) {}

  getAllDoctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(`${this.baseUrl}/all`);
  }

    getDoctorById(id: number): Observable<Doctor> {
    return this.http.get<Doctor>(`${this.baseUrl}/${id}`);
  }

  updateDoctor(id: number, doctor: Partial<Doctor>): Observable<Doctor> {
    return this.http.put<Doctor>(`${this.baseUrl}/edit/${id}`, doctor);
  }

}
