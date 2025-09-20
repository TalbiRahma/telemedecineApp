import { Component, OnInit } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { RegisterRequest } from '../../models/auth/register-request';
import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { Authentication } from '../../services/auth/authentication';
import { Router } from '@angular/router';
import { VerificationRequest } from '../../models/auth/verification-request';
import { Specialty } from '../../models/specialty';
import { SpecialtyService } from '../../services/specialty/specialty-service';
import { DoctorRegisterRequest } from '../../models/auth/doctor-register-request';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-register',
  imports: [NgIf, FormsModule, NgFor],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})

export class Register implements OnInit {

  registerRequest: RegisterRequest = {
    firstname: '',
    lastname: '',
    email: '',
    password: '',
    role: 'PATIENT',
    mfaEnabled: false
  };

  doctorRegisterRequest: DoctorRegisterRequest = {
   
  };

  authResponse: AuthenticationResponse = {
     accessToken: '',
    refreshToken: ''
  };
  message = '';
  otpCode = '';
  specialties: Specialty[] = [];
  isLoading = false; 
  errorMessage = ''; 
  selectedFile: File | null = null;

  constructor(
    private authService: Authentication,
    private router: Router,
    private specialtyService: SpecialtyService,
    private http: HttpClient
  ){
  }

   ngOnInit(): void {
    this.loadSpecialties(); // Load specialties on component initialization
  }

  loadSpecialties(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.specialtyService.getAllSpecialties().subscribe({
      next: (data) => {
        this.specialties = data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading specialties:', error);
        this.errorMessage = 'Failed to load specialties. Please try again.';
        this.isLoading = false;
      }
    })
  }

  registerUser() {
    this.message = '';
    this.registerRequest.role = 'PATIENT';
    this.authService.register(this.registerRequest)
      .subscribe({
        next: (response) => {
            this.authResponse = response;
            if (response.accessToken) {
              localStorage.setItem('token', response.accessToken);
            } 
            //
            if (this.registerRequest.mfaEnabled === false ){
               this.message = 'Account created successfullt\nYou will be redirected to the login page in 3 secondes'
            setTimeout(() => {
              this.router.navigate(['welcome']);
            }, 3000)
            }else {
              console.log('email', this.registerRequest.email);
              this.router.navigate(['two-fa'], {
               queryParams: { 
                email: this.registerRequest.email, 
                secretImageUri: response.secretImageUri 
              } 
            });
            }
        },
        error: (err) => {
          console.error(err);
          this.message = 'Registration failed. Please try again.';
        }
      });
    }


  registerDoctor() {
  this.message = '';
  
  this.doctorRegisterRequest.mfaEnabled = this.registerRequest.mfaEnabled;

  this.doctorRegisterRequest.role = 'DOCTOR';
  if (!this.doctorRegisterRequest.firstname || 
      !this.doctorRegisterRequest.lastname || 
      !this.doctorRegisterRequest.email || 
      !this.doctorRegisterRequest.password || 
      !this.doctorRegisterRequest.licenseNumber || 
      !this.doctorRegisterRequest.specialtyId ) {
    this.message = 'Please fill in all required fields';
    return;
  }


  if (!this.selectedFile) {
    this.message = 'Please select a license file';
    return;
  }

  this.authService.doctorRegister(this.doctorRegisterRequest, this.selectedFile).subscribe({
    next: (response: AuthenticationResponse) => {
      this.message = 'Doctor account created successfully!\nRedirecting to login page...';
      
      if (response.accessToken) {
        localStorage.setItem('token', response.accessToken);
      }

      if (this.doctorRegisterRequest.mfaEnabled === false ){
               this.message = 'Account created successfullt\nYou will be redirected to the login page in 3 secondes'
            setTimeout(() => {
              this.router.navigate(['welcome']);
            }, 3000)
            }else {
              console.log('email:', this.doctorRegisterRequest.email);
              console.log('secretImageUri:', response.secretImageUri );
              this.router.navigate(['two-fa'], {
               queryParams: { 
                email: this.doctorRegisterRequest.email, 
                secretImageUri: response.secretImageUri 
              } 
            });
          }
    },
    error: (err) => {
      console.error('Doctor registration failed:', err);
      this.message = err?.error?.message || 'Doctor registration failed. Please try again.';
    }
  });
}

  verifyTfa() {
    this.message = '';
    const verifyRequest: VerificationRequest = {
      email: this.registerRequest.email,
      code: this.otpCode
    };
    console.log('Sending verification request:', verifyRequest);
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

  onFileSelected(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.selectedFile = target.files[0];
    }
  }
} 