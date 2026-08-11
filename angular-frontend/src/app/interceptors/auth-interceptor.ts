import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, finalize, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { AuthenticationResponse } from '../models/auth/authentication-response';
import { Authentication } from '../services/auth/authentication';

let refreshInProgress$: Observable<AuthenticationResponse> | null = null;
let redirectingToLogin = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(Authentication);
  const hasStorage = typeof window !== 'undefined' && !!window.localStorage;
  const path = req.url.split('?')[0];

  const isRefreshRequest = path.endsWith('/api/v1/auth/refresh-token');
  const isPublicAuthRequest =
    path.endsWith('/api/v1/auth/authenticate') ||
    path.includes('/api/v1/auth/register') ||
    path.endsWith('/api/v1/auth/mfa/verify') ||
    path.endsWith('/api/v1/auth/mfa/enroll/verify') ||
    path.endsWith('/api/v1/auth/mfa/enroll/resume') ||
    path.endsWith('/api/v1/auth/forgot-password') ||
    path.endsWith('/api/v1/auth/reset-password');
  const isPublicSpecialtyRequest =
    req.method === 'GET' && /\/api\/v1\/specialties\/(all|\d+)$/.test(path);
  const isPublicRequest = isRefreshRequest || isPublicAuthRequest || isPublicSpecialtyRequest;

  if (isPublicRequest) {
    // refresh-token carries its own refresh token header from Authentication.refreshToken().
    return next(req);
  }

  const accessToken = hasStorage ? localStorage.getItem('authToken') : null;
  if (accessToken && authService.isTokenExpired()) {
    return refreshAndRetry(req, next, authService, hasStorage);
  }

  const authenticatedRequest = accessToken ? withToken(req, accessToken) : req;
  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) return throwError(() => error);
      return refreshAndRetry(req, next, authService, hasStorage, error);
    })
  );
};

function refreshAndRetry(
  originalRequest: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: Authentication,
  hasStorage: boolean,
  originalError?: HttpErrorResponse
) {
  const refresh$ = getOrStartRefresh(authService, hasStorage);
  if (!refresh$) return clearSessionAndFail(originalError, hasStorage);

  return refresh$.pipe(
    catchError((refreshError) => clearSessionAndFail(refreshError, hasStorage)),
    switchMap((response) => next(withToken(originalRequest, response.accessToken!)))
  );
}

function getOrStartRefresh(
  authService: Authentication,
  hasStorage: boolean
): Observable<AuthenticationResponse> | null {
  if (refreshInProgress$) return refreshInProgress$;

  const request$ = authService.refreshToken();
  if (!request$) return null;

  refreshInProgress$ = request$.pipe(
    tap((response) => {
      if (!response?.accessToken) throw new Error('Refresh response did not contain an access token');
      if (hasStorage) {
        localStorage.setItem('authToken', response.accessToken);
        if (response.refreshToken) localStorage.setItem('refreshToken', response.refreshToken);
      }
    }),
    finalize(() => (refreshInProgress$ = null)),
    shareReplay({ bufferSize: 1, refCount: false })
  );
  return refreshInProgress$;
}

function withToken(request: HttpRequest<unknown>, token: string) {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function clearSessionAndFail(error: unknown, hasStorage: boolean) {
  if (hasStorage) {
    localStorage.removeItem('authToken');
    localStorage.removeItem('refreshToken');
    if (!redirectingToLogin) {
      redirectingToLogin = true;
      window.location.href = '/';
    }
  }
  return throwError(() => error ?? new Error('Authentication required'));
}
