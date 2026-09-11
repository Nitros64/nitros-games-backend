variable "aws_region" {
  description = "AWS region containing the production runtime and its dependencies."
  type        = string
  default     = "eu-west-1"

  validation {
    condition     = var.aws_region == "eu-west-1"
    error_message = "Production runtime is restricted to eu-west-1."
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

variable "runtime_enabled" {
  description = "Whether disposable production runtime resources should exist. False is the safe hibernated state."
  type        = bool
  default     = false
}

variable "api_domain_name" {
  description = "Public HTTPS hostname for the production Spring Boot API."
  type        = string
  default     = "api.nitrosgames64.com"

  validation {
    condition     = var.api_domain_name == "api.nitrosgames64.com"
    error_message = "The production runtime API hostname must be api.nitrosgames64.com."
  }
}

variable "instance_type" {
  description = "Initial size of the single production application instance."
  type        = string
  default     = "t3.small"

  validation {
    condition     = var.instance_type == "t3.small"
    error_message = "Delivery 8B.6 intentionally creates one t3.small instance."
  }
}

variable "root_volume_size_gib" {
  description = "Encrypted disposable EC2 root volume size in GiB."
  type        = number
  default     = 20

  validation {
    condition     = var.root_volume_size_gib == 20
    error_message = "Delivery 8B.6 fixes the root volume at 20 GiB."
  }
}

variable "runtime_subnet_key" {
  description = "Stable foundation public-subnet key used by the first runtime instance."
  type        = string
  default     = "a"

  validation {
    condition     = var.runtime_subnet_key == "a"
    error_message = "Delivery 8B.6 places the first runtime instance in public subnet a."
  }
}

variable "ecr_repository_name" {
  description = "Existing bootstrap-owned ECR repository used for application image pulls."
  type        = string
  default     = "nitros-games-backend"

  validation {
    condition     = can(regex("^[a-z0-9]+(?:[._/-][a-z0-9]+)*$", var.ecr_repository_name))
    error_message = "ecr_repository_name must be a valid private ECR repository name."
  }
}

variable "github_owner" {
  description = "GitHub organization or user trusted by the production deployment role."
  type        = string
  default     = "Nitros64"

  validation {
    condition     = var.github_owner == "Nitros64"
    error_message = "Production CD is restricted to the Nitros64 GitHub owner."
  }
}

variable "github_repository" {
  description = "GitHub repository trusted by the production deployment role."
  type        = string
  default     = "nitros-games-backend"

  validation {
    condition     = var.github_repository == "nitros-games-backend"
    error_message = "Production CD is restricted to the nitros-games-backend repository."
  }
}

variable "github_environment" {
  description = "Protected GitHub Environment encoded in the production OIDC subject."
  type        = string
  default     = "production"

  validation {
    condition     = var.github_environment == "production"
    error_message = "Production CD must use the production GitHub Environment."
  }
}

variable "docker_compose_version" {
  description = "Pinned Docker Compose release installed during host bootstrap."
  type        = string
  default     = "v5.1.4"

  validation {
    condition     = can(regex("^v[0-9]+\\.[0-9]+\\.[0-9]+$", var.docker_compose_version))
    error_message = "docker_compose_version must be a semantic version prefixed with v."
  }
}

variable "docker_compose_sha256" {
  description = "SHA-256 for the pinned Linux x86_64 Docker Compose binary."
  type        = string
  default     = "33b208d7e76639db742fae84b966cc01dacae58ca3fc4dabbc907045aefdf0c4"

  validation {
    condition     = can(regex("^[0-9a-f]{64}$", var.docker_compose_sha256))
    error_message = "docker_compose_sha256 must be a lowercase SHA-256 digest."
  }
}

variable "additional_tags" {
  description = "Additional tags applied to every taggable runtime resource."
  type        = map(string)
  default     = {}
}
