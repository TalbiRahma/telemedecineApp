import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { PatientDashboardHome } from './patient-dashboard-home';

describe('PatientDashboardHome', () => {
  let component: PatientDashboardHome;
  let fixture: ComponentFixture<PatientDashboardHome>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientDashboardHome],
      providers: [provideHttpClient()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PatientDashboardHome);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
