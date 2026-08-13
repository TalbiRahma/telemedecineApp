import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { DoctorOverview } from './doctor-overview';

describe('DoctorOverview', () => {
  let component: DoctorOverview;
  let fixture: ComponentFixture<DoctorOverview>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DoctorOverview],
      providers: [provideHttpClient()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DoctorOverview);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
