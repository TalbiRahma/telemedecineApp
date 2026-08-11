import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminPatientListPage } from './admin-patient-list-page';

describe('AdminPatientListPage', () => {
  let component: AdminPatientListPage;
  let fixture: ComponentFixture<AdminPatientListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPatientListPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminPatientListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
