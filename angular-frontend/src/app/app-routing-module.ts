import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {Login} from "./pages/login/login";
import {Register} from './pages/register/register';
import {Welcome} from './pages/welcome/welcome';
import {authGuard} from './services/auth/auth-guard';

const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: 'register',
    component: Register
  },
  {
    path: 'welcome',
    component: Welcome,
    canActivate: [authGuard]
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
