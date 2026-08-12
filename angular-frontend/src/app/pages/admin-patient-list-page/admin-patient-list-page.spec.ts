import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminPatientsListPage } from './admin-patient-list-page';

describe('AdminPatientListPage', () => {
  let component: AdminPatientsListPage;
  let fixture: ComponentFixture<AdminPatientsListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPatientsListPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminPatientsListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
