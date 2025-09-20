import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthenticationRequest } from '../../models/auth/authentication-request';
import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { VerificationRequest } from '../../models/auth/verification-request';
import { RegisterRequest } from '../../models/auth/register-request';
import { DoctorRegisterRequest } from '../../models/auth/doctor-register-request';
import { jwtDecode } from 'jwt-decode';

@Injectable({
  providedIn: 'root'
})
export class Authentication {

  private baseUrl = 'http://localhost:8080/api/v1/auth'

  constructor(
    private http: HttpClient
  ) {

  }

  register(
    registerRequest: RegisterRequest
  ){
   return this.http.post<AuthenticationResponse>
   (`${this.baseUrl}/register`, registerRequest);
  }
  
  doctorRegister(
  doctorRegisterRequest: DoctorRegisterRequest,
  file?: File
) {
  const formData = new FormData();
  formData.append('request', JSON.stringify(doctorRegisterRequest)); // 👈 must match @RequestPart("request")
  if (file) {
    formData.append('image', file); // 👈 must match @RequestParam("image")
  }

  return this.http.post<AuthenticationResponse>(
    `${this.baseUrl}/register/doctor`,
    formData
  );
}


  login(
    authRequest: AuthenticationRequest
  ){
    return this.http.post<AuthenticationResponse>
    (`${this.baseUrl}/authenticate`, authRequest);
  }

  verifyCode(
    VerificationRequest: VerificationRequest
  ){
     console.log('Sending verification request:', VerificationRequest);
    return this.http.post<AuthenticationResponse>
    (`${this.baseUrl}/verify`, VerificationRequest)
  }

 refreshToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return null;

  return this.http.post<AuthenticationResponse>(
    'http://localhost:8080/api/v1/auth/refresh-token',
    {},
    {
      headers: {
        Authorization: `Bearer ${refreshToken}`
      }
    }
  );
}


  

    getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  getDecodedToken(): any {
    const token = this.getToken();
    if (!token) return null;
    
    try {
      return jwtDecode(token);
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  }

   getUserRole(): string | null {
    const decodedToken = this.getDecodedToken();
    return decodedToken?.role || null;
  }

   isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isDoctor(): boolean {
    return this.getUserRole() === 'DOCTOR';
  }

  isPatient(): boolean {
    return this.getUserRole() === 'PATIENT';
  }
}
