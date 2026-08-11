export interface AuthenticationResponse{
    accessToken?: string;
    refreshToken?: string;
    mfaEnabled?: boolean;
    mfaRequired?: boolean;
    mfaEnrollmentRequired?: boolean;
    mfaChallengeToken?: string;
    qrCodeImageUri?: string;
    secretImageUri?: string;
}
