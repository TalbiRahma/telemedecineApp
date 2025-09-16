import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Welcome } from './pages/welcome/welcome';
import { Register } from './pages/register/register';
import { TwoFaPage } from './pages/two-fa-page/two-fa-page';

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
        path: 'welcome',
        component: Welcome
    }

];
