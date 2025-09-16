export interface DoctorRegisterRequest{
    firstname?: string;
    lastname?: string;
    email?: string;
    password?: string;
    role?: string; // or use enum if you have Role defined
    licenseNumber?: string;
    specialtyId?: number;
    certificationUrl?: string; // optional since it might be set by backend
    mfaEnabled?: boolean;
}