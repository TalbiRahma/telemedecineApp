import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Authentication } from '../services/auth/authentication';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(Authentication);
  const router = inject(Router);

  if (typeof window === 'undefined') return true;

  if (!auth.isLoggedIn()) {
    return router.parseUrl('/'); // login route
  }

  return true;
};
