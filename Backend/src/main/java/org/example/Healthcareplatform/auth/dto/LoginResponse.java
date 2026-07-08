package org.example.Healthcareplatform.auth.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresInMs;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean rememberMe;
}
