import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminDoctorsListPage } from './admin-doctors-list-page';

describe('AdminDoctorsListPage', () => {
  let component: AdminDoctorsListPage;
  let fixture: ComponentFixture<AdminDoctorsListPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDoctorsListPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminDoctorsListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
