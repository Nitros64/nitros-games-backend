/**
 * Host-image and file-storage module.
 *
 * <p>The module owns host-image metadata, its HTTP API and persistence, plus
 * the application port and local-filesystem adapter used to store image files.
 * Other modules may reference the public storage domain types, but must not
 * depend on storage API, application, persistence or infrastructure types.</p>
 */
package com.nitros64.nitros_games_backend.storage;
