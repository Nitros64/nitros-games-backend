provider "aws" {
  region = var.aws_region

  default_tags {
    tags = merge(
      var.additional_tags,
      {
        Project     = var.project_name
        Environment = var.environment
        ManagedBy   = "Terraform"
        Component   = "production-foundation"
      }
    )
  }
}

# Route 53 Domains created the public hosted zone without tags. This dedicated
# provider preserves that existing metadata during adoption instead of adding
# the foundation default tags as an incidental import change.
provider "aws" {
  alias  = "route53_untagged"
  region = var.aws_region
}
