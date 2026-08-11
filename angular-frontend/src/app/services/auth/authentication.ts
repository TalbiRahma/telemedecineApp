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

  private baseUrl = '/api/v1/auth'

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
  file: File
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

  forgotPassword(email: string) {
    return this.http.post<{ message: string }>(`${this.baseUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string, confirmPassword: string) {
    return this.http.post<{ message: string }>(`${this.baseUrl}/reset-password`, {
      token,
      newPassword,
      confirmPassword
    });
  }

  verifyCode(
    verificationRequest: VerificationRequest
  ){
    return this.http.post<AuthenticationResponse>
    (`${this.baseUrl}/mfa/verify`, verificationRequest)
  }

  verifyEnrollmentCode(verificationRequest: VerificationRequest) {
    return this.http.post<AuthenticationResponse>(
      `${this.baseUrl}/mfa/enroll/verify`, verificationRequest
    );
  }

  resumeEnrollment(challengeToken: string) {
    return this.http.post<AuthenticationResponse>(
      `${this.baseUrl}/mfa/enroll/resume`, { challengeToken }
    );
  }

  beginEnrollment() {
    return this.http.post<AuthenticationResponse>(`${this.baseUrl}/mfa/enroll`, {});
  }

  beginMfa(response: AuthenticationResponse): boolean {
    if (typeof window === 'undefined' || !window.sessionStorage || !response.mfaChallengeToken) {
      return false;
    }
    sessionStorage.setItem('mfaChallengeToken', response.mfaChallengeToken);
    sessionStorage.setItem('mfaEnrollmentRequired', String(!!response.mfaEnrollmentRequired));
    return true;
  }

  getMfaChallenge(): { challengeToken: string; enrollment: boolean } | null {
    if (typeof window === 'undefined' || !window.sessionStorage) return null;
    const challengeToken = sessionStorage.getItem('mfaChallengeToken');
    if (!challengeToken) return null;
    return {
      challengeToken,
      enrollment: sessionStorage.getItem('mfaEnrollmentRequired') === 'true'
    };
  }

  clearMfaChallenge(): void {
    if (typeof window === 'undefined' || !window.sessionStorage) return;
    sessionStorage.removeItem('mfaChallengeToken');
    sessionStorage.removeItem('mfaEnrollmentRequired');
  }

  storeSession(response: AuthenticationResponse): boolean {
    if (typeof window === 'undefined' || !window.localStorage || !response.accessToken) return false;
    localStorage.setItem('authToken', response.accessToken);
    if (response.refreshToken) localStorage.setItem('refreshToken', response.refreshToken);
    this.clearMfaChallenge();
    return true;
  }

 refreshToken() {
  if (typeof window === 'undefined' || !window.localStorage) return null;
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return null;

  return this.http.post<AuthenticationResponse>(
    `${this.baseUrl}/refresh-token`,
    {},
    {
      headers: {
        Authorization: `Bearer ${refreshToken}`
      }
    }
  );
}

logout() {
  return this.http.post(
    `${this.baseUrl}/logout`,
    {},
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('authToken')}`
      }
    }
  ).subscribe({
    next: () => {
      // Effacer les tokens côté frontend
      localStorage.removeItem('authToken');
      localStorage.removeItem('refreshToken');
      this.clearMfaChallenge();
      window.location.href = '/'; // redirect vers login
    },
    error: (err) => {
      console.error('Logout error:', err);
      // Même si backend échoue, effacer les tokens localement
      localStorage.removeItem('authToken');
      localStorage.removeItem('refreshToken');
      this.clearMfaChallenge();
      window.location.href = '/';
    }
  });
}



  

    getToken(): string | null {
    if (typeof window === 'undefined' || !window.localStorage) return null;
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

  getUserId(): number | null {
  const decodedToken = this.getDecodedToken();
  return decodedToken?.id || null;
}

isLoggedIn(): boolean {
  const token = this.getToken();
  if (!token) return false;

  const decoded = this.getDecodedToken();
  if (!decoded?.exp) return false;

  // exp بالثواني
  return Date.now() < decoded.exp * 1000;
}

isTokenExpired(): boolean {
  const decoded = this.getDecodedToken();
  if (!decoded?.exp) return true;
  return Date.now() >= decoded.exp * 1000;
}


}
