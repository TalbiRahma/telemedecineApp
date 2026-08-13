import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { DoctorDashboardPage } from './doctor-dashboard-page';

describe('DoctorDashboardPage', () => {
  let component: DoctorDashboardPage;
  let fixture: ComponentFixture<DoctorDashboardPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DoctorDashboardPage],
      providers: [provideHttpClient(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DoctorDashboardPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
