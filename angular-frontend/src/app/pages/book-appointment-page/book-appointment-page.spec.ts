import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BookAppointmentPage } from './book-appointment-page';

describe('BookAppointmentPage', () => {
  let component: BookAppointmentPage;
  let fixture: ComponentFixture<BookAppointmentPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BookAppointmentPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BookAppointmentPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
