provider "aws" {
  region = var.aws_region

  default_tags {
    tags = merge(
      var.additional_tags,
      {
        Project     = var.project_name
        Environment = var.environment
        ManagedBy   = "Terraform"
        Component   = "staging-compute"
      }
    )
  }
}
