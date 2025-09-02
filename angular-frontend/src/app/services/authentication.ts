import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {RegisterRequest} from '../pages/models/register-request';
import {AuthenticationResponse} from '../pages/models/authentication-response';
import {VerificationRequest} from '../pages/models/Verification-request';
import {AuthenticationRequest} from '../pages/models/authentication-request';

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

  login(
    authRequest: AuthenticationRequest
  ){
    return this.http.post<AuthenticationResponse>
    (`${this.baseUrl}/authenticate`, authRequest);
  }
  verifyCode(verificationRequest: VerificationRequest) {
    return this.http.post<AuthenticationResponse>(`${this.baseUrl}/verify`, verificationRequest);
  }

}
