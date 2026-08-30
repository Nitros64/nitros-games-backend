variable "aws_region" {
  description = "AWS region where the ECR repository will be created."
  type        = string

  validation {
    condition     = can(regex("^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$", var.aws_region))
    error_message = "aws_region must be a valid AWS region name, for example eu-west-1."
  }
}

variable "project_name" {
  description = "Stable project name used for resources and tags."
  type        = string
  default     = "nitros-games-backend"

  validation {
    condition     = can(regex("^[a-z0-9]+(?:[._-][a-z0-9]+)*$", var.project_name))
    error_message = "project_name must use lowercase letters, numbers, dots, underscores or hyphens."
  }
}

variable "github_owner" {
  description = "GitHub organization or account that owns the repository. This value is case-sensitive."
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$", var.github_owner))
    error_message = "github_owner must be a valid GitHub account or organization name."
  }
}

variable "github_repository" {
  description = "GitHub repository allowed to publish images."
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]{1,100}$", var.github_repository))
    error_message = "github_repository must be a valid GitHub repository name."
  }
}

variable "github_branch" {
  description = "Only workflows running from this branch may assume the ECR publisher role."
  type        = string
  default     = "main"

  validation {
    condition = (
      trimspace(var.github_branch) != ""
      && !startswith(var.github_branch, "refs/")
      && !strcontains(var.github_branch, ":")
    )
    error_message = "github_branch must be a branch name without the refs/ prefix or colons."
  }
}

variable "existing_github_oidc_provider_arn" {
  description = "ARN of an existing GitHub Actions OIDC provider in the AWS account. Leave null to create it."
  type        = string
  default     = null

  validation {
    condition = (
      var.existing_github_oidc_provider_arn == null
      || can(regex(
        "^arn:[^:]+:iam::[0-9]{12}:oidc-provider/token\\.actions\\.githubusercontent\\.com$",
        var.existing_github_oidc_provider_arn
      ))
    )
    error_message = "existing_github_oidc_provider_arn must be a GitHub Actions OIDC provider ARN."
  }
}

variable "max_image_count" {
  description = "Maximum number of tagged and untagged images retained in ECR."
  type        = number
  default     = 30

  validation {
    condition     = var.max_image_count >= 10 && var.max_image_count <= 500
    error_message = "max_image_count must be between 10 and 500."
  }
}

variable "untagged_image_days" {
  description = "Days to retain untagged images before ECR expires them."
  type        = number
  default     = 7

  validation {
    condition     = var.untagged_image_days >= 1 && var.untagged_image_days <= 30
    error_message = "untagged_image_days must be between 1 and 30."
  }
}

variable "additional_tags" {
  description = "Additional tags applied to every resource."
  type        = map(string)
  default     = {}
}
