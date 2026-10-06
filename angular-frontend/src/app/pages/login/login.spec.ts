import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { AuthenticationResponse } from '../../models/auth/authentication-response';
import { Authentication } from '../../services/auth/authentication';
import { Login } from './login';

describe('Login demo accounts', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authentication: jasmine.SpyObj<Authentication>;
  let router: Router;

  const publicDemoConfig = {
    enabled: true,
    adminEmail: 'superadmin@medilink.demo',
    adminPassword: 'AdminDemoPassword',
    doctorEmail: 'doctor01@medilink.demo',
    doctorPassword: 'UserDemoPassword',
    patientEmail: 'patient01@medilink.demo',
    patientPassword: 'UserDemoPassword'
  };

  beforeEach(async () => {
    window.__MEDILINK_DEMO_CONFIG__ = publicDemoConfig;
    authentication = jasmine.createSpyObj<Authentication>('Authentication', [
      'login', 'clearMfaChallenge', 'beginMfa', 'storeSession', 'getUserRole'
    ]);

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: Authentication, useValue: authentication }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    delete window.__MEDILINK_DEMO_CONFIG__;
  });

  it('shows the collapsible demo section only when demo mode is configured', () => {
    expect(fixture.nativeElement.querySelector('.demo-access')).not.toBeNull();

    window.__MEDILINK_DEMO_CONFIG__ = { enabled: false };
    const disabledFixture = TestBed.createComponent(Login);
    disabledFixture.detectChanges();
    expect(disabledFixture.nativeElement.querySelector('.demo-access')).toBeNull();
  });

  [
    ['ADMIN', 'superadmin@medilink.demo', 'AdminDemoPassword'],
    ['DOCTOR', 'doctor01@medilink.demo', 'UserDemoPassword'],
    ['PATIENT', 'patient01@medilink.demo', 'UserDemoPassword']
  ].forEach(([role, email, password]) => {
    it(`fills the normal login form and automatically authenticates ${role}`, () => {
      authentication.login.and.returnValue(new Subject<AuthenticationResponse>());
      component.demoExpanded = true;
      fixture.detectChanges();
      const cards = fixture.debugElement.queryAll(By.css('.demo-card'));
      const card = cards.find((candidate) => candidate.nativeElement.textContent.includes(role));

      card?.query(By.css('.demo-use-button')).triggerEventHandler('click');
      fixture.detectChanges();

      expect(component.authRequest).toEqual({ email, password });
      expect(authentication.login).toHaveBeenCalledOnceWith({ email, password });
    });
  });

  it('prevents duplicate demo login requests and exposes the loading state', () => {
    authentication.login.and.returnValue(new Subject<AuthenticationResponse>());
    component.demoExpanded = true;
    fixture.detectChanges();
    const buttons = fixture.debugElement.queryAll(By.css('.demo-use-button'));

    buttons[1].triggerEventHandler('click');
    fixture.detectChanges();
    buttons[1].triggerEventHandler('click');

    expect(authentication.login).toHaveBeenCalledTimes(1);
    expect(component.isLoading).toBeTrue();
    expect(buttons.every((button) => button.nativeElement.disabled)).toBeTrue();
    expect(buttons[1].nativeElement.textContent.trim()).toBe('Signing in…');
    expect(fixture.nativeElement.querySelector('.sign-in').disabled).toBeTrue();
  });

  it('restores demo controls after a failed login and keeps the credentials populated', () => {
    authentication.login.and.returnValue(throwError(() => new Error('Unauthorized')));
    const account = component.demoConfiguration.accounts[2];

    component.useDemoAccount(account);
    fixture.detectChanges();

    expect(component.isLoading).toBeFalse();
    expect(component.authRequest).toEqual({ email: account.email, password: account.password });
    expect(component.loginError).toBe('Invalid email or password. Please try again.');
    component.demoExpanded = true;
    fixture.detectChanges();
    const buttons = fixture.debugElement.queryAll(By.css('.demo-use-button'));
    expect(buttons.every((button) => !button.nativeElement.disabled)).toBeTrue();
    expect(buttons[2].nativeElement.textContent.trim()).toBe('Use');
  });

  it('continues to submit the normal form through the existing authentication flow', () => {
    authentication.login.and.returnValue(of({ accessToken: 'token' }));
    authentication.storeSession.and.returnValue(true);
    authentication.getUserRole.and.returnValue('PATIENT');
    component.authRequest = { email: 'patient01@medilink.demo', password: 'UserDemoPassword' };

    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit');

    expect(authentication.login).toHaveBeenCalledOnceWith(component.authRequest);
    expect(authentication.storeSession).toHaveBeenCalledOnceWith({ accessToken: 'token' });
  });

  it('keeps the existing role-based redirect for a successful demo login', () => {
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    authentication.login.and.returnValue(of({ accessToken: 'token' }));
    authentication.storeSession.and.returnValue(true);
    authentication.getUserRole.and.returnValue('DOCTOR');

    component.useDemoAccount(component.demoConfiguration.accounts[1]);

    expect(navigate).toHaveBeenCalledOnceWith(['/doctor-dashboard/overview']);
  });

  it('keeps the existing MFA challenge flow for a demo login', () => {
    const mfaResponse: AuthenticationResponse = {
      mfaRequired: true,
      mfaChallengeToken: 'challenge'
    };
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    authentication.login.and.returnValue(of(mfaResponse));
    authentication.beginMfa.and.returnValue(true);

    component.useDemoAccount(component.demoConfiguration.accounts[0]);

    expect(authentication.beginMfa).toHaveBeenCalledOnceWith(mfaResponse);
    expect(authentication.storeSession).not.toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledOnceWith(['/two-fa']);
  });

  it('does not allow demo authentication when demo mode is disabled', () => {
    window.__MEDILINK_DEMO_CONFIG__ = { enabled: false };
    const disabledFixture = TestBed.createComponent(Login);
    const disabledComponent = disabledFixture.componentInstance;
    disabledFixture.detectChanges();

    disabledComponent.useDemoAccount(component.demoConfiguration.accounts[0]);

    expect(disabledFixture.nativeElement.querySelector('.demo-access')).toBeNull();
    expect(authentication.login).not.toHaveBeenCalled();
  });
});
