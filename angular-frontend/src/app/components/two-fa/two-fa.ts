import { Component } from '@angular/core';
import { VerificationRequest } from '../../models/auth/verification-request';
import { AuthenticationRequest } from '../../models/auth/authentication-request';
import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { Authentication } from '../../services/auth/authentication';
import { Router } from '@angular/router';

@Component({
  selector: 'app-two-fa',
  imports: [],
  templateUrl: './two-fa.html',
  styleUrl: './two-fa.scss'
})
export class TwoFa {


   authRequest: AuthenticationRequest = {};
  otpCode: any;
  authResponse: AuthenticationResponse = {};

  constructor(
    private authService: Authentication,
    private router: Router
  ) {

  }

  
    verifyCode(){
      const verifyRequest: VerificationRequest ={
        email: this.authRequest.email,
        code: this.otpCode
      };
      this.authService.verifyCode(verifyRequest)
      .subscribe({
        next: (response) => {
          localStorage.setItem('token', response.accessToken as string);
          this.router.navigate(['welcome'])
        }
      })
    }

}
