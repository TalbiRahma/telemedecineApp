import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PatientDashboardHome } from './patient-dashboard-home';

describe('PatientDashboardHome', () => {
  let component: PatientDashboardHome;
  let fixture: ComponentFixture<PatientDashboardHome>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientDashboardHome]
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
