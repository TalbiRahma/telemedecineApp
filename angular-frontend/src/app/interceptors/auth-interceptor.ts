import { HttpInterceptorFn } from '@angular/common/http';

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
};