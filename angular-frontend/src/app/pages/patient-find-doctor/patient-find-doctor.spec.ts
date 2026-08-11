import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PatientFindDoctor } from './patient-find-doctor';

describe('PatientFindDoctor', () => {
  let component: PatientFindDoctor;
  let fixture: ComponentFixture<PatientFindDoctor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientFindDoctor]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PatientFindDoctor);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
