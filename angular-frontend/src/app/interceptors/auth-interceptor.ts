import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Authentication } from '../services/auth/authentication';
import { catchError, switchMap, throwError } from 'rxjs';

// auth.interceptor.ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(Authentication);

  let authToken: string | null = localStorage.getItem('authToken');
  
  console.log('Interceptor - Current token:', authToken);
  
  if (authToken) {
    // Check if token is expired before using it
    const decodedToken = authService.getDecodedToken();
    if (decodedToken && decodedToken.exp) {
      const expirationTime = decodedToken.exp * 1000; // Convert to milliseconds
      const currentTime = Date.now();
      
      console.log('Token expires at:', new Date(expirationTime));
      console.log('Current time:', new Date(currentTime));
      console.log('Token valid:', currentTime < expirationTime);
      
      if (currentTime >= expirationTime) {
        console.log('Token expired, attempting refresh...');
        // Token is expired, don't use it
        authToken = null;
      }
    }

    if (authToken) {
      req = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${authToken}`)
      });
    }
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.log('Interceptor error:', error.status, error.message);
      
      if (error.status === 401) {
        console.log('Attempting token refresh...');
        return authService.refreshToken()!.pipe(
          switchMap((res) => {
            console.log('Refresh successful, new token:', res.accessToken);
            localStorage.setItem('authToken', res.accessToken);

            const newReq = req.clone({
              headers: req.headers.set('Authorization', `Bearer ${res.accessToken}`)
            });
            return next(newReq);
          }),
          catchError(refreshErr => {
            console.log('Refresh failed:', refreshErr);
            localStorage.removeItem('authToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/login';
            return throwError(() => refreshErr);
          })
        );
      }

      return throwError(() => error);
    })
  );
};




/*import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  
  let authToken: string | null = null;
  
  // Safely check if localStorage is available (browser environment)
  try {
    if (typeof window !== 'undefined' && typeof window.localStorage !== 'undefined') {
      authToken = localStorage.getItem('authToken');
    }
  } catch (error) {
    console.warn('localStorage is not available in this environment');
  }

  if (authToken) {
    const authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${authToken}`)
    });
    return next(authReq);
  }
  
  return next(req);
};*/