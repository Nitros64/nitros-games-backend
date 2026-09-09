/**
 * Cross-cutting HTTP security configuration.
 *
 * <p>Read endpoints are public, while API mutations require the externally
 * issued OAuth2 access token containing the {@code ADMIN} application role.
 * Token trust is based on issuer, signing keys, client identity and API
 * resource targeting rather than on a specific identity-provider SDK. CORS is
 * controlled centrally through an explicit origin allowlist.</p>
 */
package com.nitros64.nitros_games_backend.security;
