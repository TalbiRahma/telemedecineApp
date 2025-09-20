import { Component } from '@angular/core';
import { AuthenticationRequest } from '../../models/auth/authentication-request';
import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { Authentication } from '../../services/auth/authentication';
import { ActivatedRoute, Router } from "@angular/router";
import { VerificationRequest } from '../../models/auth/verification-request';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms'; 


@Component({
  selector: 'app-login',
  imports: [NgIf, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  authRequest: AuthenticationRequest = {};
  optCode: any;
  authResponse: AuthenticationResponse = {
    accessToken: '',
    refreshToken: ''
  };
  email: string = '';
  secretImageUri: string = '';
  otpCode: string = '';
  message: string = '';
  showTwoFAModal: boolean = false; // Add this property
  pendingEmail: string = ''; // Add this property
  pendingSecretImageUri: string = '';

  constructor(
    private route: ActivatedRoute,
    private authService: Authentication,
    private router: Router
  ) {

  }

 /*authenticate(){
    this.authService.login(this.authRequest)
    .subscribe({
      next: (response) =>{
        this.authResponse = response;
        if(!this.authResponse.mfaEnabled){
          localStorage.setItem('token', response.accessToken as string);
          this.router.navigate(['welcome']);
        }
      }
    })
  }*/

    ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      this.secretImageUri = params['secretImageUri'] || '';
    });
  }

  authenticate(){
    this.authService.login(this.authRequest).subscribe({
  next: (response) => {
    console.log("Login response:", response);

    if (response.accessToken) {
              localStorage.setItem('authToken', response.accessToken);
    }

    if (response.refreshToken) {
        localStorage.setItem('refreshToken', response.refreshToken);
      }

    if (response.mfaEnabled) {
      // Show Tailwind modal instead of alert
          this.showTwoFAModal = true;
          this.pendingEmail = this.authRequest.email || '';
          this.pendingSecretImageUri = response.secretImageUri || ''; // Store the secretImageUri

      console.log('email', this.authRequest.email);
              
    } else {
      if (response.accessToken) {
        localStorage.setItem('authToken', response.accessToken);
      
      if (response.refreshToken) {
        localStorage.setItem('refreshToken', response.refreshToken);
      }
          this.redirectBasedOnRole();
      } else {
        alert("Login successful but no token received");
      }
    }
  },
  error: (err) => {
    console.error("Login error:", err);
    alert("Invalid credentials. Please try again.");
  }
});

  }

  
  /*verifyCode(){
    const verifyRequest: VerificationRequest ={
      email: this.authRequest.email,
      code: this.optCode
    };
    this.authService.verifyCode(verifyRequest)
    .subscribe({
      next: (response) => {
        localStorage.setItem('token', response.accessToken as string);
        this.router.navigate(['welcome'])
      }
    })
  }
*/

closeTwoFAModal() {
  this.showTwoFAModal = false;
}

goTo2FA() {
  this.showTwoFAModal = false;
  this.router.navigate(['two-fa'], {
    queryParams: { email: this.pendingEmail,
        secretImageUri: this.pendingSecretImageUri }
  });
}
redirectBasedOnRole() {
  const role = this.authService.getUserRole();
  
  switch (role) {
    case 'ADMIN':
      this.router.navigate(['/admin-dashboard/overview']);
      break;
    case 'DOCTOR':
      this.router.navigate(['/doctor/dashboard']);
      break;
    case 'PATIENT':
      this.router.navigate(['/patient/dashboard']);
      break;
    default:
      this.router.navigate(['/welcome']);
      break;
  }
}
}
