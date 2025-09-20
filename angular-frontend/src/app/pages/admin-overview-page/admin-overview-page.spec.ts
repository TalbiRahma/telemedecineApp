import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminOverviewPage } from './admin-overview-page';

describe('AdminOverviewPage', () => {
  let component: AdminOverviewPage;
  let fixture: ComponentFixture<AdminOverviewPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminOverviewPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminOverviewPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
