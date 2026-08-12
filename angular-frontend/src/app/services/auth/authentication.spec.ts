import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { Authentication } from './authentication';

describe('Authentication', () => {
  let service: Authentication;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient()] });
    service = TestBed.inject(Authentication);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
