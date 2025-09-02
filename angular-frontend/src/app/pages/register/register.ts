import { Component } from '@angular/core';
import {RegisterRequest} from '../models/register-request';
import {AuthenticationResponse} from '../models/authentication-response';
import {Authentication} from '../../services/authentication';
import { Router } from "@angular/router";
import {VerificationRequest} from '../models/Verification-request';

@Component({
  selector: 'app-register',
  standalone: false,
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register {

  registerRequest: RegisterRequest = {};
  authResponse: AuthenticationResponse = {};
  message = '';
  otpCode = '';

  constructor(
    private authService: Authentication,
    private router: Router
  ) {
  }

  registerUser() {
    this.message = '';
    this.authService.register(this.registerRequest)
      .subscribe({
        next: (response) => {
          if (response) {
            this.authResponse = response;
          }else {
            //
            this.message = 'Account created successfullt\nYou will be redirected to the login page in 3 secondes'
            setTimeout(() => {
              this.router.navigate(['login']);
            }, 3000)
          }
        }
      })
  }

  verifyTfa() {
    this.message = '';
    const verifyRequest: VerificationRequest = {
      email: this.registerRequest.email,
      code: this.otpCode
    };
    this.authService.verifyCode(verifyRequest)
      .subscribe({
        next: (response)=> {
          this.message = 'Account created successfullt\nYou will be redirected to the welcome page in 3 secondes'
          setTimeout(() => {
            localStorage.setItem('token', response.accessToken as string);
            this.router.navigate(['welcome']);
          }, 3000);
        }
      })
  }
}
