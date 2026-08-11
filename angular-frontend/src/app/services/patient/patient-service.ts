import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, shareReplay, tap } from 'rxjs';
import { PatientDto } from '../../models/PatientDto';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/patients';
  private meRequest?: Observable<PatientDto>;
  private currentPatient?: PatientDto;


  getMe(): Observable<PatientDto> {
    if (this.currentPatient) return of(this.currentPatient);
    if (!this.meRequest) {
      this.meRequest = this.http.get<PatientDto>(`${this.baseUrl}/me`).pipe(
        tap(patient => this.currentPatient = patient),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.meRequest;
  }

  updateMe(payload: PatientDto): Observable<PatientDto> {
    return this.http.put<PatientDto>(`${this.baseUrl}/me`, payload).pipe(
      tap(patient => {
        this.currentPatient = patient;
        this.meRequest = of(patient).pipe(shareReplay({ bufferSize: 1, refCount: false }));
      })
    );
  }

 getById(id: number) {
  return this.http.get<PatientDto>(`${this.baseUrl}/${id}`);
}

getAllPatients(): Observable<PatientDto[]> {
  return this.http.get<PatientDto[]>(`${this.baseUrl}`);
}


}
