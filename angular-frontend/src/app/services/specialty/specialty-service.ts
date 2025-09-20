import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Specialty } from '../../models/specialty';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SpecialtyService {
  
  private baseUrl = 'http://localhost:8080/api/v1/specialties'

  constructor(
    private http: HttpClient
  ) {

  }

   getAllSpecialties(): Observable<Specialty[]> {
    return this.http.get<Specialty[]>(`${this.baseUrl}/all`);
  }

  getById(id: number): Observable<Specialty> {
    return this.http.get<Specialty>(`${this.baseUrl}/${id}`);
  }

  add(specialty: Partial<Specialty>): Observable<Specialty> {
    return this.http.post<Specialty>(`${this.baseUrl}/add`, specialty);
  }

   update(id: number, specialty: Partial<Specialty>): Observable<Specialty> {
    return this.http.put<Specialty>(`${this.baseUrl}/edit/${id}`, specialty);
  }

   delete(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/delete/${id}`, { responseType: 'text' });
  }
}
