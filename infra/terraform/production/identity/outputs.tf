output "user_pool_id" {
  description = "Production Cognito user pool identifier."
  value       = aws_cognito_user_pool.production.id
}

output "issuer_uri" {
  description = "Trusted issuer required by the Spring Boot resource server."
  value       = "https://${aws_cognito_user_pool.production.endpoint}"
}

output "jwk_set_uri" {
  description = "Trusted Cognito JWK Set endpoint required by Spring Boot."
  value       = "https://${aws_cognito_user_pool.production.endpoint}/.well-known/jwks.json"
}

output "api_resource_id" {
  description = "Stable API resource identifier used by JWT validation."
  value       = var.api_resource_identifier
}

output "api_access_scope" {
  description = "Scope requested by the Angular public client."
  value       = local.access_scope
}

output "api_admin_scope" {
  description = "Scope reserved for a future least-privilege M2M client."
  value       = local.admin_scope
}

output "angular_client_id" {
  description = "Public client ID for the future Angular Authorization Code + PKCE flow."
  value       = aws_cognito_user_pool_client.angular.id
}

output "authorization_endpoint" {
  description = "AWS-managed HTTPS authorization endpoint used by the future Angular client."
  value       = "https://${aws_cognito_user_pool_domain.production.domain}.auth.${var.aws_region}.amazoncognito.com/oauth2/authorize"
}

output "token_endpoint" {
  description = "AWS-managed HTTPS token endpoint."
  value       = "https://${aws_cognito_user_pool_domain.production.domain}.auth.${var.aws_region}.amazoncognito.com/oauth2/token"
}
