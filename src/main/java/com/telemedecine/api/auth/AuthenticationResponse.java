package com.telemedecine.api.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {

    @JsonProperty("accessToken")
    private String accessToken;
    private String refreshToken;
    private boolean mfaEnabled;
    private boolean mfaRequired;
    private boolean mfaEnrollmentRequired;
    private String mfaChallengeToken;
    private String qrCodeImageUri;
    /** Kept for wire compatibility; populated only during enrollment, never normal login. */
    @Deprecated
    private String secretImageUri;

}
