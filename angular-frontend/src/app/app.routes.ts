import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Welcome } from './pages/welcome/welcome';
import { Register } from './pages/register/register';
import { TwoFaPage } from './pages/two-fa-page/two-fa-page';
import { MfaSetupPage } from './pages/mfa-setup-page/mfa-setup-page';
import { AdminDashboardPage } from './pages/admin-dashboard-page/admin-dashboard-page';
import { AdminDoctorsListPage } from './pages/admin-doctors-list-page/admin-doctors-list-page';
import { AdminOverviewPage } from './pages/admin-overview-page/admin-overview-page';
import { AdminSpecialtiesListPage } from './pages/admin-specialties-list-page/admin-specialties-list-page';
import { PatientDashboardPage } from './pages/patient-dashboard-page/patient-dashboard-page';
import { DoctorDashboardPage } from './pages/doctor-dashboard-page/doctor-dashboard-page';
import { DoctorOverview } from './pages/doctor-overview/doctor-overview';
import { DoctorAppointments } from './pages/doctor-appointments/doctor-appointments';
import { DoctorAvailability } from './pages/doctor-availability/doctor-availability';
import { PatientFindDoctor } from './pages/patient-find-doctor/patient-find-doctor';
import { PatientDashboardHome } from './pages/patient-dashboard-home/patient-dashboard-home';
import { BookAppointmentPage } from './pages/book-appointment-page/book-appointment-page';
import { SymptomsCheckerPage } from './pages/symptoms-checker-page/symptoms-checker-page';
import { PatientAppointmentsPage } from './pages/patient-appointments-page/patient-appointments-page';
import { PatientProfilePage } from './pages/patient-profile-page/patient-profile-page';
import { DoctorProfilePage } from './pages/doctor-profile-page/doctor-profile-page';
import { AdminPatientsListPage } from './pages/admin-patient-list-page/admin-patient-list-page';
import { AdminSettingsPage } from './pages/admin-settings-page/admin-settings-page';
import { roleGuard } from './guards/role-guard';
import { authGuard } from './guards/auth-guard';
import { ForgotPassword } from './pages/forgot-password/forgot-password';
import { ResetPassword } from './pages/reset-password/reset-password';

export const routes: Routes = [
    {
        path: '',
        component: Login
    },
    {
        path: 'register',
        component: Register
    },
    {
        path: 'forgot-password',
        component: ForgotPassword
    },
    {
        path: 'reset-password',
        component: ResetPassword
    },
    {
        path: 'two-fa',
        component: TwoFaPage
    },
    {
        path: 'mfa-setup',
        component: MfaSetupPage
    },
    {
        path: 'admin-dashboard',
        component: AdminDashboardPage,
        canActivate: [authGuard, roleGuard],
          data: { roles: ['ADMIN'] },
        children: [          
            { path: '', redirectTo: 'overview', pathMatch: 'full' },
            { path: 'overview', component: AdminOverviewPage, data: { title: 'Dashboard Overview' } },
            { path: 'users/doctors', component: AdminDoctorsListPage, data: { title: 'Doctors Management' } },
            { path: 'specialties-management', component: AdminSpecialtiesListPage, data: { title: 'Specialties Management' } },
            { path: 'users/patients', component: AdminPatientsListPage, data: { title: 'Patients Management' } },
            { path: 'settings', component: AdminSettingsPage, data: { title: 'Settings' } }

        ]
    },
    /*{
        path: 'patient-dashboard',
        component: PatientDashboardPage,
         children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },

      {
        path: 'dashboard',
        component: PatientDashboardHome   // dashboard content
      },

      {
        path: 'find-doctor',
        component: PatientFindDoctor
      },
      {
        path: 'book-appointment/:doctorId',
        component: BookAppointmentPage
      },

      {
        path: 'appointments',
        component: PatientAppointmentsPage
      },


      {
        path: 'profile',
        component: PatientProfilePage
      },

      {
        path: 'symptoms-checker',
        component: SymptomsCheckerPage
      }
    ]

    },*/
    {
  path: 'patient-dashboard',
  component: PatientDashboardPage,
  canActivate: [authGuard, roleGuard],
  data: { roles: ['PATIENT'] },
  children: [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    { path: 'dashboard', component: PatientDashboardHome },
    { path: 'find-doctor', component: PatientFindDoctor },
    { path: 'book-appointment/:doctorId', component: BookAppointmentPage },
    { path: 'appointments', component: PatientAppointmentsPage },
    { path: 'profile', component: PatientProfilePage },
    { path: 'symptoms-checker', component: SymptomsCheckerPage }
  ]
},

    {
        path: 'doctor-dashboard',
        component: DoctorDashboardPage,
        
  canActivate: [authGuard, roleGuard],
  data: { roles: ['DOCTOR'] },
         children: [
            { path: 'overview', component: DoctorOverview },
            /*{ path: 'patients', component: DoctorPatientsComponent },*/
            { path: 'appointments', component: DoctorAppointments },
            { path: 'availability', component: DoctorAvailability },
            { path: 'profile', component: DoctorProfilePage},
            { path: '', redirectTo: 'overview', pathMatch: 'full' }
        ]

    },
    {
        path: 'welcome',
        component: Welcome
    }

];
