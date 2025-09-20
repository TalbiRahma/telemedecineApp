export interface AuthenticationResponse{
    accessToken: string;
    refreshToken: string;
    mfaEnabled?: boolean;
    secretImageUri?: string;
}