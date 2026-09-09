variable "aws_region" {
  description = "AWS region containing the production data tier."
  type        = string
  default     = "eu-west-1"

  validation {
    condition     = var.aws_region == "eu-west-1"
    error_message = "Production foundation and its remote state are in eu-west-1."
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
  description = "Deployment environment name."
  type        = string
  default     = "production"

  validation {
    condition     = var.environment == "production"
    error_message = "This Terraform root is restricted to production."
  }
}

variable "mysql_engine_version" {
  description = "Pinned RDS for MySQL 8.4 patch version verified in eu-west-1."
  type        = string
  default     = "8.4.10"

  validation {
    condition     = can(regex("^8\\.4\\.[0-9]+$", var.mysql_engine_version))
    error_message = "mysql_engine_version must remain on the explicitly reviewed MySQL 8.4 line."
  }
}

variable "final_snapshot_identifier" {
  description = "Stable final snapshot name. Change it explicitly before a later deletion if that name already exists."
  type        = string
  default     = "nitros-games-backend-production-mysql-final"

  validation {
    condition = (
      length(var.final_snapshot_identifier) >= 1
      && length(var.final_snapshot_identifier) <= 255
      && can(regex("^[a-z](?:[a-z0-9-]*[a-z0-9])?$", var.final_snapshot_identifier))
      && !strcontains(var.final_snapshot_identifier, "--")
    )
    error_message = "final_snapshot_identifier must be a valid lowercase RDS snapshot identifier."
  }
}

variable "additional_tags" {
  description = "Additional tags applied to every taggable resource."
  type        = map(string)
  default     = {}
}
