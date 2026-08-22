/**
 * Cross-cutting HTTP security configuration.
 *
 * <p>Read endpoints are public, while API mutations require the externally
 * configured administrator account. CORS is controlled centrally through an
 * explicit origin allowlist.</p>
 */
package com.nitros64.nitros_games_backend.security;
