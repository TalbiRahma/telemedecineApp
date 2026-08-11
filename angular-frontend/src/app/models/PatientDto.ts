export interface PatientDto {
  id?: number;
  firstname?: string;
  lastname?: string;
  email?: string;     // readonly on UI
  phone?: string;
  dateOfBirth?: string; // 'YYYY-MM-DD'
  gender?: string;      // 'MALE' | 'FEMALE' | 'OTHER' أو نص
}
