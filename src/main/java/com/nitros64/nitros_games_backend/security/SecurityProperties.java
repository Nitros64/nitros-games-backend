package com.nitros64.nitros_games_backend.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security")
public class SecurityProperties {

    @NotEmpty
    private List<@NotBlank String> allowedOrigins = new ArrayList<>();

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
