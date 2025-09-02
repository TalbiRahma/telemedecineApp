import { Component } from '@angular/core';
import {AuthenticationResponse} from '../models/authentication-response';
import {Authentication} from '../../services/authentication';
import { Router } from "@angular/router";
import {AuthenticationRequest} from '../models/authentication-request';
import {VerificationRequest} from "../models/Verification-request";

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {

  authRequest: AuthenticationRequest = {};
  otpCode: any;
  authResponse: AuthenticationResponse = {};

  constructor(
    private authService: Authentication,
    private router: Router
  ) {

  }

  authenticate() {
    this.authService.login(this.authRequest)
      .subscribe({
        next: (response) => {
          this.authResponse = response;
          if (!this.authResponse.mfaEnabled) {
            localStorage.setItem('token', response.accessToken as string);
            this.router.navigate(['welcome']);
          }
        }
      });
  }

  verifyCode() {
    const verifyRequest: VerificationRequest = {
      email: this.authRequest.email,
      code: this.otpCode
    };
    this.authService.verifyCode(verifyRequest)
      .subscribe({
        next: (response) => {
          localStorage.setItem('token', response.accessToken as string);
          this.router.navigate(['welcome']);
        }
      });
  }
}
