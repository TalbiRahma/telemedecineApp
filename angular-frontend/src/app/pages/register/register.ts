import { Component, OnInit } from '@angular/core';
import { DecimalPipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { RegisterRequest } from '../../models/auth/register-request';
import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { Authentication } from '../../services/auth/authentication';
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { Specialty } from '../../models/specialty';
import { SpecialtyService } from '../../services/specialty/specialty-service';
import { DoctorRegisterRequest } from '../../models/auth/doctor-register-request';

@Component({
  selector: 'app-register',
  imports: [NgIf, FormsModule, NgFor, RouterLink, DecimalPipe],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})

export class Register implements OnInit {
  activeRole: 'PATIENT' | 'DOCTOR' = 'PATIENT';

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
  isSpecialtiesLoading = false;
  errorMessage = ''; 
  selectedFile: File | null = null;

  constructor(
    private authService: Authentication,
    private router: Router,
    private specialtyService: SpecialtyService
  ){
  }

   ngOnInit(): void {
    this.loadSpecialties(); // Load specialties on component initialization
  }

  loadSpecialties(): void {
    this.isSpecialtiesLoading = true;
    this.errorMessage = '';

    this.specialtyService.getAllSpecialties().subscribe({
      next: (data) => {
        this.specialties = data;
        this.isSpecialtiesLoading = false;
      },
      error: (error) => {
        console.error('Error loading specialties:', error);
        this.errorMessage = 'Failed to load specialties. Please try again.';
        this.isSpecialtiesLoading = false;
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
            if (response.mfaRequired) {
              this.startMfa(response);
            } else if (this.authService.storeSession(response)) {
               this.message = 'Account created successfullt\nYou will be redirected to the login page in 3 secondes'
              setTimeout(() => this.redirectBasedOnRole(), 3000);
            } else {
              this.message = 'Registration did not return a valid session. Please sign in.';
            }
        },
        error: (err) => {
          console.error(err);
          this.message = 'Registration failed. Please try again.';
        }
      });
    }


  registerDoctor() {
  if (this.isLoading) {
    return;
  }
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

  this.isLoading = true;
  this.authService.doctorRegister(this.doctorRegisterRequest, this.selectedFile).subscribe({
    next: (response: AuthenticationResponse) => {
      this.isLoading = false;
      this.message = 'Doctor account created successfully!\nRedirecting to login page...';
      
      if (response.mfaRequired) {
        this.startMfa(response);
      } else if (this.authService.storeSession(response)) {
               this.message = 'Account created successfullt\nYou will be redirected to the login page in 3 secondes'
        setTimeout(() => this.redirectBasedOnRole(), 3000);
      } else {
        this.message = 'Registration did not return a valid session. Please sign in.';
      }
    },
    error: (err) => {
      this.isLoading = false;
      console.error('Doctor registration failed:', err);
      const backendMessage = typeof err?.error?.message === 'string'
        ? err.error.message
        : null;
      this.message = backendMessage || 'Doctor registration failed. Please try again.';
    }
  });
}

  private startMfa(response: AuthenticationResponse): void {
    if (!this.authService.beginMfa(response)) {
      this.message = 'Unable to start two-factor setup. Please sign in and try again.';
      return;
    }
    this.router.navigate(['/mfa-setup'], {
      state: { qrCodeImageUri: response.qrCodeImageUri || null }
    });
  }

  onFileSelected(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.selectedFile = target.files[0];
    }
  }

  clearSelectedFile(input: HTMLInputElement): void {
    this.selectedFile = null;
    input.value = '';
  }

  redirectBasedOnRole() {
  const role = this.authService.getUserRole();
  
  switch (role) {
    case 'ADMIN':
      this.router.navigate(['/admin-dashboard/overview']);
      break;
    case 'DOCTOR':
      this.router.navigate(['/doctor-dashboard/overview']);
      break;
    case 'PATIENT':
      this.router.navigate(['/patient-dashboard']);
      break;
    default:
      this.router.navigate(['/welcome']);
      break;
  }
}
}
