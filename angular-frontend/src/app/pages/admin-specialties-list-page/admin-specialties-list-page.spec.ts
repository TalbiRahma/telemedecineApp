import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminSpecialtiesListPage } from './admin-specialties-list-page';

describe('AdminSpecialtiesListPage', () => {
  let component: AdminSpecialtiesListPage;
  let fixture: ComponentFixture<AdminSpecialtiesListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminSpecialtiesListPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminSpecialtiesListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
