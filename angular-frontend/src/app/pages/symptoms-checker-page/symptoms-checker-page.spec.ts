import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SymptomsCheckerPage } from './symptoms-checker-page';

describe('SymptomsCheckerPage', () => {
  let component: SymptomsCheckerPage;
  let fixture: ComponentFixture<SymptomsCheckerPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SymptomsCheckerPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SymptomsCheckerPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
