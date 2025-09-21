import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Welcome } from './pages/welcome/welcome';
import { Register } from './pages/register/register';
import { TwoFaPage } from './pages/two-fa-page/two-fa-page';
import { AdminDashboardPage } from './pages/admin-dashboard-page/admin-dashboard-page';
import { AdminDoctorsListPage } from './pages/admin-doctors-list-page/admin-doctors-list-page';
import { AdminOverviewPage } from './pages/admin-overview-page/admin-overview-page';
import { AdminSpecialtiesListPage } from './pages/admin-specialties-list-page/admin-specialties-list-page';
import { PatientDashboardPage } from './pages/patient-dashboard-page/patient-dashboard-page';
import { DoctorDashboardPage } from './pages/doctor-dashboard-page/doctor-dashboard-page';

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
        path: 'two-fa',
        component: TwoFaPage
    },
    {
        path: 'admin-dashboard',
        component: AdminDashboardPage,
        children: [          
            { path: '', redirectTo: 'overview', pathMatch: 'full' },
            { path: 'overview', component: AdminOverviewPage },  
            { path: 'users/doctors', component: AdminDoctorsListPage },
            { path: 'specialties-management', component: AdminSpecialtiesListPage }
        ]
    },
    {
        path: 'patient-dashboard',
        component: PatientDashboardPage,
        
    },
    {
        path: 'doctor-dashboard',
        component: DoctorDashboardPage,
        
    },
    {
        path: 'welcome',
        component: Welcome
    }

];
