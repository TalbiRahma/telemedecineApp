package com.telemedecine.api.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.datatransfer.StringSelection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthenticationResponse {

    @JsonProperty("accessToken")
    private String accessToken;
    private String refreshToken;
    private boolean mfaEnabled;
    private String secretImageUri;

}
