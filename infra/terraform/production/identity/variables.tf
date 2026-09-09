variable "aws_region" {
  description = "AWS region containing the production Cognito user pool."
  type        = string
  default     = "eu-west-1"

  validation {
    condition     = var.aws_region == "eu-west-1"
    error_message = "Production identity is restricted to eu-west-1."
  }
}

variable "project_name" {
  description = "Stable project name used for resource names and tags."
  type        = string
  default     = "nitros-games-backend"

  validation {
    condition     = can(regex("^[a-z0-9]+(?:[._-][a-z0-9]+)*$", var.project_name))
    error_message = "project_name must use lowercase letters, numbers, dots, underscores or hyphens."
  }
}

variable "environment" {
  description = "Environment name; this root is production-only."
  type        = string
  default     = "production"

  validation {
    condition     = var.environment == "production"
    error_message = "This Terraform root is restricted to production."
  }
}

variable "api_resource_identifier" {
  description = "Stable OAuth resource-server identifier used as the API scope prefix."
  type        = string
  default     = "https://api.nitrosgames64.com"

  validation {
    condition     = can(regex("^https://[a-z0-9.-]+$", var.api_resource_identifier))
    error_message = "api_resource_identifier must be an HTTPS origin without a path or trailing slash."
  }
}

variable "angular_callback_urls" {
  description = "Exact HTTPS callback URLs for the future Angular Authorization Code + PKCE client."
  type        = list(string)
  default     = ["https://nitrosgames64.com/auth/callback"]

  validation {
    condition = length(var.angular_callback_urls) > 0 && alltrue([
      for url in var.angular_callback_urls : can(regex("^https://[^?#]+$", url))
    ])
    error_message = "At least one exact HTTPS Angular callback URL is required."
  }
}

variable "angular_logout_urls" {
  description = "Exact HTTPS post-logout URLs for the future Angular client."
  type        = list(string)
  default     = ["https://nitrosgames64.com/"]

  validation {
    condition = length(var.angular_logout_urls) > 0 && alltrue([
      for url in var.angular_logout_urls : can(regex("^https://[^?#]+$", url))
    ])
    error_message = "At least one exact HTTPS Angular logout URL is required."
  }
}

variable "cognito_domain_prefix" {
  description = "Globally unique prefix for the AWS-managed Cognito HTTPS domain."
  type        = string
  default     = "nitros-games-production-529601496188"

  validation {
    condition     = can(regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$", var.cognito_domain_prefix))
    error_message = "cognito_domain_prefix must be a valid lowercase Cognito prefix."
  }
}

variable "additional_tags" {
  description = "Additional tags applied to every taggable identity resource."
  type        = map(string)
  default     = {}
}
