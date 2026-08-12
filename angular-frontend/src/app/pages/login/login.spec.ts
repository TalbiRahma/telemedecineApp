import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Authentication } from '../../services/auth/authentication';
import { Login } from './login';

describe('Login demo accounts', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authentication: jasmine.SpyObj<Authentication>;

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
    it(`fills the normal login form for ${role} without submitting`, () => {
      component.demoExpanded = true;
      fixture.detectChanges();
      const cards = fixture.debugElement.queryAll(By.css('.demo-card'));
      const card = cards.find((candidate) => candidate.nativeElement.textContent.includes(role));

      card?.query(By.css('.demo-use-button')).triggerEventHandler('click');
      fixture.detectChanges();

      expect(component.authRequest).toEqual({ email, password });
      expect(authentication.login).not.toHaveBeenCalled();
    });
  });

  it('continues to submit through the existing authentication service', () => {
    authentication.login.and.returnValue(of({ accessToken: 'token' }));
    authentication.storeSession.and.returnValue(true);
    authentication.getUserRole.and.returnValue('PATIENT');
    component.authRequest = { email: 'patient01@medilink.demo', password: 'UserDemoPassword' };

    component.authenticate();

    expect(authentication.login).toHaveBeenCalledWith(component.authRequest);
  });
});
