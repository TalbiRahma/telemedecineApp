import { Component, OnInit } from '@angular/core';
import { AuthenticationRequest } from '../../models/auth/authentication-request';
import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { Authentication } from '../../services/auth/authentication';
import { ActivatedRoute, Router } from '@angular/router';
import { VerificationRequest } from '../../models/auth/verification-request';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-two-fa-page',
  imports: [FormsModule],
  templateUrl: './two-fa-page.html',
  styleUrl: './two-fa-page.scss'
})
export class TwoFaPage  implements OnInit{

  authRequest: AuthenticationRequest = {};
  otpCode: any ;
  email: string = '';
  secretImageUri: any; 
  authResponse: AuthenticationResponse = {
     accessToken: '',
    refreshToken: ''
  };
    isLoading: boolean = false;

  constructor(
    private authService: Authentication,
    private router: Router,
     private route: ActivatedRoute
  ) {

  }

   ngOnInit() {
    // Get data from query parameters instead of navigation state
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || '';
      this.secretImageUri = params['secretImageUri'] || '';
      
      console.log('Email from params:', this.email);
      console.log('Secret URI from params:', this.secretImageUri);
      
      if (!this.email) {
        console.error('No email provided for 2FA');
        // Redirect back to registration if no email
        this.router.navigate(['register']);
      }
    });
  }

    /*verifyCode(){
      const verifyRequest: VerificationRequest ={
        email: this.email,
        code: this.otpCode
      };
      this.authService.verifyCode(verifyRequest)
      .subscribe({
        next: (response) => {
          console.log('2FA verified successfully', response);
          //localStorage.setItem('token', response.accessToken as string);
          this.router.navigate(['welcome'])
        },
        error: (err) => {
          console.error(err);
           console.log('email', this.authRequest.email);
          alert('Invalid code, please try again.');
        }
      })
    }*/

     /* verifyCode() {
    if (!this.email) {
      console.error('Email is not available for verification');
      alert('Email information is missing. Please try registering again.');
      this.router.navigate(['register']);
      return;
    }
    
    if (!this.otpCode || this.otpCode.length < 6) {
      alert('Please enter a valid 6-digit code');
      return;
    }
    
    this.isLoading = true;
    
    const verifyRequest: VerificationRequest = {
      email: this.email,
      code: this.otpCode
    };
    
    this.authService.verifyCode(verifyRequest)
      .subscribe({
        next: (response) => {
          console.log('2FA verified successfully', response);
          this.isLoading = false;
          
          if (response.accessToken) {
            localStorage.setItem('token', response.accessToken);
            this.router.navigate(['welcome']);
          } else {
            alert('Verification successful but no access token received');
          }
        },
        error: (err) => {
          console.error('Verification error:', err);
          this.isLoading = false;
          alert('Invalid code, please try again.');
        }
      });
  }*/

verifyCode() {
  if (!this.email) {
    console.error('Email is not available for verification');
    alert('Email information is missing. Please try registering again.');
    this.router.navigate(['register']);
    return;
  }
  
  if (!this.otpCode || this.otpCode.length < 6) {
    alert('Please enter a valid 6-digit code');
    return;
  }
  
  this.isLoading = true;
  
  const verifyRequest: VerificationRequest = {
    email: this.email,
    code: this.otpCode
  };
  
  this.authService.verifyCode(verifyRequest)
    .subscribe({
      next: (response) => {
        console.log('2FA verified successfully', response);
        this.isLoading = false;
        
        if (response.accessToken) {
          localStorage.setItem('token', response.accessToken);
          this.redirectBasedOnRole();
        } else {
          // Handle case where token is missing but verification succeeded
          console.warn('Verification successful but no access token received');
          // You might want to proceed to login or request a token
          this.router.navigate([''], { 
            queryParams: { message: 'Verification successful. Please login.' } 
          });
        }
      },
      error: (err) => {
        console.error('Verification error:', err);
        this.isLoading = false;
        alert('Invalid code, please try again.');
      }
    });
}

redirectBasedOnRole() {
  const role = this.authService.getUserRole();
  
  switch (role) {
    case 'ADMIN':
      this.router.navigate(['/admin/dashboard']);
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
