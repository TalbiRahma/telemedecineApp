import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Welcome } from './pages/welcome/welcome';
import { Register } from './pages/register/register';
import { TwoFaPage } from './pages/two-fa-page/two-fa-page';
import { AdminDashboardPage } from './pages/admin-dashboard-page/admin-dashboard-page';
import { AdminDoctorsListPage } from './pages/admin-doctors-list-page/admin-doctors-list-page';
import { AdminOverviewPage } from './pages/admin-overview-page/admin-overview-page';

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
            { path: 'users/doctors', component: AdminDoctorsListPage }
        ]
    },
    {
        path: 'welcome',
        component: Welcome
    }

];
