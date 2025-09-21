import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PatientDashboardPage } from './patient-dashboard-page';

describe('PatientDashboardPage', () => {
  let component: PatientDashboardPage;
  let fixture: ComponentFixture<PatientDashboardPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientDashboardPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PatientDashboardPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
