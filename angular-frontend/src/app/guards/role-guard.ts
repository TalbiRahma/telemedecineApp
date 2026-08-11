import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Authentication } from '../services/auth/authentication';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(Authentication);
  const router = inject(Router);

  if (typeof window === 'undefined') return true;

  if (!auth.isLoggedIn()) {
    return router.parseUrl('/');
  }

  const allowedRoles = route.data?.['roles'] as string[] | undefined;
  if (!allowedRoles?.length) return true;

  const userRole = auth.getUserRole(); // ADMIN/DOCTOR/PATIENT
  if (!userRole || !allowedRoles.includes(userRole)) {
    return router.parseUrl('/'); // ولا '/' حسب ما تحب
  }

  return true;
};
