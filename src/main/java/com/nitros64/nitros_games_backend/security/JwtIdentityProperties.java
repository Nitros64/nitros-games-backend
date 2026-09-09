package com.nitros64.nitros_games_backend.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security.jwt")
public class JwtIdentityProperties {

    @NotBlank
    private String issuerUri;

    @NotBlank
    private String jwkSetUri;

    @NotBlank
    private String resourceId;

    @NotBlank
    private String accessScope;

    @NotBlank
    private String adminScope;

    @NotEmpty
    private List<@NotBlank String> allowedClientIds = new ArrayList<>();

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getAccessScope() {
        return accessScope;
    }

    public void setAccessScope(String accessScope) {
        this.accessScope = accessScope;
    }

    public String getAdminScope() {
        return adminScope;
    }

    public void setAdminScope(String adminScope) {
        this.adminScope = adminScope;
    }

    public List<String> getAllowedClientIds() {
        return allowedClientIds;
    }

    public void setAllowedClientIds(List<String> allowedClientIds) {
        this.allowedClientIds = new ArrayList<>(allowedClientIds);
    }

    @AssertTrue(message = "JWT access and administrator scopes must belong to the configured resource")
    public boolean areScopesOwnedByResource() {
        if (resourceId == null || resourceId.isBlank()) {
            return true;
        }
        String prefix = resourceId + "/";
        return accessScope != null
                && adminScope != null
                && accessScope.startsWith(prefix)
                && adminScope.startsWith(prefix)
                && !accessScope.equals(adminScope);
    }
}
