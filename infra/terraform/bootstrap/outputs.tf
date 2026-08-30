output "aws_region" {
  description = "AWS region selected for the delivery foundation."
  value       = var.aws_region
}

output "ecr_repository_arn" {
  description = "ARN of the application ECR repository."
  value       = aws_ecr_repository.application.arn
}

output "ecr_repository_name" {
  description = "Name passed to the ECR publishing workflow."
  value       = aws_ecr_repository.application.name
}

output "ecr_repository_url" {
  description = "URL used to tag and push the application image."
  value       = aws_ecr_repository.application.repository_url
}

output "github_actions_ecr_role_arn" {
  description = "Role assumed by the main branch GitHub Actions workflow."
  value       = aws_iam_role.github_ecr_publisher.arn
}

output "github_oidc_provider_arn" {
  description = "GitHub Actions OIDC provider used by the publisher role."
  value       = local.github_oidc_provider_arn
}

output "trusted_github_subject" {
  description = "Exact GitHub OIDC subject allowed to assume the publisher role."
  value       = local.github_subject
}
