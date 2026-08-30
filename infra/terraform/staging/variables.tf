variable "aws_region" {
  description = "AWS region where the staging environment runs."
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

variable "environment" {
  description = "Environment name used in resource names and tags."
  type        = string
  default     = "staging"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,15}$", var.environment))
    error_message = "environment must be a short lowercase identifier."
  }
}

variable "instance_type" {
  description = "EC2 size. t3.small provides enough memory for the API and MySQL demo stack."
  type        = string
  default     = "t3.small"

  validation {
    condition     = contains(["t3.small", "t3.medium"], var.instance_type)
    error_message = "instance_type must be t3.small or t3.medium."
  }
}

variable "root_volume_size_gib" {
  description = "Encrypted, disposable root volume size in GiB."
  type        = number
  default     = 16

  validation {
    condition     = var.root_volume_size_gib >= 12 && var.root_volume_size_gib <= 50
    error_message = "root_volume_size_gib must be between 12 and 50 GiB."
  }
}

variable "vpc_cidr" {
  description = "Private CIDR for the isolated staging VPC."
  type        = string
  default     = "10.42.0.0/24"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 1))
    error_message = "vpc_cidr must be valid IPv4 CIDR notation."
  }
}

variable "subnet_cidr" {
  description = "Public subnet used by the single staging instance."
  type        = string
  default     = "10.42.0.0/25"

  validation {
    condition     = can(cidrhost(var.subnet_cidr, 1))
    error_message = "subnet_cidr must be valid IPv4 CIDR notation."
  }
}

variable "ecr_repository_name" {
  description = "Existing ECR repository from which the instance can pull images."
  type        = string
  default     = "nitros-games-backend"

  validation {
    condition     = can(regex("^[a-z0-9]+(?:[._/-][a-z0-9]+)*$", var.ecr_repository_name))
    error_message = "ecr_repository_name must be a valid private ECR repository name."
  }
}

variable "github_owner" {
  description = "Case-sensitive GitHub account that owns the repository."
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$", var.github_owner))
    error_message = "github_owner must be a valid GitHub account or organization name."
  }
}

variable "github_repository" {
  description = "GitHub repository allowed to deploy staging."
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]{1,100}$", var.github_repository))
    error_message = "github_repository must be a valid GitHub repository name."
  }
}

variable "github_branch" {
  description = "Only workflows on this branch may assume the staging deployer role."
  type        = string
  default     = "main"

  validation {
    condition = (
      trimspace(var.github_branch) != ""
      && !startswith(var.github_branch, "refs/")
      && !strcontains(var.github_branch, ":")
    )
    error_message = "github_branch must be a branch name without refs/ or colons."
  }
}

variable "github_oidc_provider_arn" {
  description = "Existing GitHub Actions OIDC provider ARN from the bootstrap root."
  type        = string

  validation {
    condition = can(regex(
      "^arn:[^:]+:iam::[0-9]{12}:oidc-provider/token\\.actions\\.githubusercontent\\.com$",
      var.github_oidc_provider_arn
    ))
    error_message = "github_oidc_provider_arn must identify the GitHub Actions provider."
  }
}

variable "docker_compose_version" {
  description = "Pinned Docker Compose plugin version installed during instance bootstrap."
  type        = string
  default     = "v5.1.4"

  validation {
    condition     = can(regex("^v[0-9]+\\.[0-9]+\\.[0-9]+$", var.docker_compose_version))
    error_message = "docker_compose_version must be a semantic version prefixed with v."
  }
}

variable "docker_compose_sha256" {
  description = "SHA-256 checksum for the pinned Linux x86_64 Docker Compose binary."
  type        = string
  default     = "33b208d7e76639db742fae84b966cc01dacae58ca3fc4dabbc907045aefdf0c4"

  validation {
    condition     = can(regex("^[0-9a-f]{64}$", var.docker_compose_sha256))
    error_message = "docker_compose_sha256 must be a lowercase SHA-256 digest."
  }
}

variable "additional_tags" {
  description = "Additional tags applied to every taggable resource."
  type        = map(string)
  default     = {}
}
