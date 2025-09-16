// fichier partagé pour tous les composants standalone
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';


export const sharedImports = [
  CommonModule,         // *ngIf, *ngFor, etc.
  FormsModule,          // [(ngModel)] template-driven forms
  ReactiveFormsModule,  // FormGroup, FormControl

];
