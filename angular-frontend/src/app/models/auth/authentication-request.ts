export interface AuthenticationRequest{
    email?: string;
    password?: string;
    mfaEnabled?: boolean;
}