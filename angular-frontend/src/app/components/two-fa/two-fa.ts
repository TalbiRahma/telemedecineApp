import { Component } from '@angular/core';
import { VerificationRequest } from '../../models/auth/verification-request';
import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { Authentication } from '../../services/auth/authentication';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-two-fa',
  imports: [NgIf],
  templateUrl: './two-fa.html',
  styleUrl: './two-fa.scss'
})
export class TwoFa {

  enabled = true;
  otpCode: any;
  authResponse: AuthenticationResponse = {
     accessToken: '',
    refreshToken: ''
  };

  constructor(
    private authService: Authentication,
    private router: Router
  ) {

  }

  
    verifyCode(){
      const verifyRequest: VerificationRequest ={
        challengeToken: this.authService.getMfaChallenge()?.challengeToken || '',
        code: String(this.otpCode || '')
      };
      this.authService.verifyCode(verifyRequest)
      .subscribe({
        next: (response) => {
          this.authService.storeSession(response);
          this.router.navigate(['welcome'])
        }
      })
    }

}
