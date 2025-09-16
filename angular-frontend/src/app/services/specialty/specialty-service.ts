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
}
