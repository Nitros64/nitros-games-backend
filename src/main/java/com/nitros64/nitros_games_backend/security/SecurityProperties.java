package com.nitros64.nitros_games_backend.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security")
public class SecurityProperties {

    @NotBlank
    private String adminUsername;

    @NotBlank
    @Size(min = 16)
    private String adminPassword;

    @NotEmpty
    private List<@NotBlank String> allowedOrigins = new ArrayList<>();

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = new ArrayList<>(allowedOrigins);
    }

    @AssertTrue(message = "CORS allowed origins cannot contain a wildcard")
    public boolean isAllowedOriginsSafe() {
        return allowedOrigins != null && allowedOrigins.stream().noneMatch("*"::equals);
    }
}
