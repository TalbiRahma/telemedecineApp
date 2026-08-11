import { Specialty } from "./specialty";

export interface Doctor {
  id: number;
  firstname : string;
  lastname : string;
  email: string;
  phone: string;
  licenseNumber: string;
  adresse: string;
  certificationUrl: string;
  state: string; 
  specialty: Specialty;
 
}