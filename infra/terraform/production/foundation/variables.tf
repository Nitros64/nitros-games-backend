variable "aws_region" {
  description = "AWS region for the production network foundation."
  type        = string
  default     = "eu-west-1"

  validation {
    condition     = can(regex("^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$", var.aws_region))
    error_message = "aws_region must be a valid AWS region name, for example eu-west-1."
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
    error_message = "This Terraform root is restricted to the production environment."
  }
}

variable "domain_name" {
  description = "Official NitrosGames production domain."
  type        = string
  default     = "nitrosgames64.com"

  validation {
    condition     = var.domain_name == "nitrosgames64.com"
    error_message = "The production foundation is restricted to nitrosgames64.com."
  }
}

variable "vpc_cidr" {
  description = "Non-overlapping IPv4 CIDR for the production VPC."
  type        = string
  default     = "10.43.0.0/16"

  validation {
    condition     = var.vpc_cidr == "10.43.0.0/16"
    error_message = "Delivery 8B.1 reserves 10.43.0.0/16 for production so it cannot overlap staging."
  }
}

variable "public_subnet_cidrs" {
  description = "CIDRs for the two public subnets, keyed by stable AZ suffix."
  type        = map(string)
  default = {
    a = "10.43.0.0/24"
    b = "10.43.1.0/24"
  }

  validation {
    condition = (
      length(var.public_subnet_cidrs) == 2
      && lookup(var.public_subnet_cidrs, "a", "") == "10.43.0.0/24"
      && lookup(var.public_subnet_cidrs, "b", "") == "10.43.1.0/24"
    )
    error_message = "Delivery 8B.1 reserves 10.43.0.0/24 and 10.43.1.0/24 for the production public subnets."
  }
}

variable "private_data_subnet_cidrs" {
  description = "CIDRs for the two private data subnets, keyed by stable AZ suffix."
  type        = map(string)
  default = {
    a = "10.43.10.0/24"
    b = "10.43.11.0/24"
  }

  validation {
    condition = (
      length(var.private_data_subnet_cidrs) == 2
      && lookup(var.private_data_subnet_cidrs, "a", "") == "10.43.10.0/24"
      && lookup(var.private_data_subnet_cidrs, "b", "") == "10.43.11.0/24"
    )
    error_message = "Delivery 8B.1 reserves 10.43.10.0/24 and 10.43.11.0/24 for the production private data subnets."
  }
}

variable "additional_tags" {
  description = "Additional tags applied to every resource."
  type        = map(string)
  default     = {}
}
