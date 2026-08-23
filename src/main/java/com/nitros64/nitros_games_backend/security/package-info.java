/**
 * Cross-cutting HTTP security configuration.
 *
 * <p>Read endpoints are public, while API mutations require the externally
 * issued OAuth2 access token containing the {@code ADMIN} realm role. CORS is
 * controlled centrally through an explicit origin allowlist.</p>
 */
package com.nitros64.nitros_games_backend.security;
