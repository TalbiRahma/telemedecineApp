import { TestBed } from '@angular/core/testing';

import { provideHttpClient } from '@angular/common/http';
import { PrediagnosticApiService } from './prediagnostic-api.service.ts';

describe('PrediagnosticApiServiceTs', () => {
  let service: PrediagnosticApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient()] });
    service = TestBed.inject(PrediagnosticApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
