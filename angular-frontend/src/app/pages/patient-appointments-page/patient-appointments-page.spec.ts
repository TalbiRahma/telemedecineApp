import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PatientAppointmentsPage } from './patient-appointments-page';

describe('PatientAppointmentsPage', () => {
  let component: PatientAppointmentsPage;
  let fixture: ComponentFixture<PatientAppointmentsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientAppointmentsPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PatientAppointmentsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
