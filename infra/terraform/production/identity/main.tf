locals {
  name_prefix  = "${var.project_name}-${var.environment}"
  access_scope = "${var.api_resource_identifier}/access"
  admin_scope  = "${var.api_resource_identifier}/admin"
}

resource "aws_cognito_user_pool" "production" {
  name                = "${local.name_prefix}-users"
  user_pool_tier      = "LITE"
  deletion_protection = "ACTIVE"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  mfa_configuration        = "OPTIONAL"

  software_token_mfa_configuration {
    enabled = true
  }

  password_policy {
    minimum_length                   = 14
    require_lowercase                = true
    require_numbers                  = true
    require_symbols                  = true
    require_uppercase                = true
    temporary_password_validity_days = 3
  }

  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  user_attribute_update_settings {
    attributes_require_verification_before_update = ["email"]
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_cognito_user_pool_domain" "production" {
  domain       = var.cognito_domain_prefix
  user_pool_id = aws_cognito_user_pool.production.id
}

resource "aws_cognito_resource_server" "api" {
  identifier   = var.api_resource_identifier
  name         = "Nitros Games API"
  user_pool_id = aws_cognito_user_pool.production.id

  scope {
    scope_name        = "access"
    scope_description = "Access the Nitros Games API"
  }

  scope {
    scope_name        = "admin"
    scope_description = "Mutate Nitros Games API resources through trusted automation"
  }
}

resource "aws_cognito_user_group" "administrators" {
  name         = "ADMIN"
  description  = "May mutate Nitros Games resources"
  user_pool_id = aws_cognito_user_pool.production.id
  precedence   = 10
}

resource "aws_cognito_user_pool_client" "angular" {
  name         = "nitros-games-web"
  user_pool_id = aws_cognito_user_pool.production.id

  generate_secret                      = false
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["openid", "email", "profile", local.access_scope]
  callback_urls                        = var.angular_callback_urls
  logout_urls                          = var.angular_logout_urls
  supported_identity_providers         = ["COGNITO"]
  explicit_auth_flows                  = ["ALLOW_REFRESH_TOKEN_AUTH"]
  prevent_user_existence_errors        = "ENABLED"
  enable_token_revocation              = true
  access_token_validity                = 15
  id_token_validity                    = 15
  refresh_token_validity               = 30

  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "days"
  }

  depends_on = [aws_cognito_resource_server.api]
}
