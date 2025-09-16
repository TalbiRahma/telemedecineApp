import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TwoFaPage } from './two-fa-page';

describe('TwoFaPage', () => {
  let component: TwoFaPage;
  let fixture: ComponentFixture<TwoFaPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TwoFaPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TwoFaPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
