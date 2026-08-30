output "aws_region" {
  description = "AWS region containing staging."
  value       = var.aws_region
}

output "instance_id" {
  description = "Instance targeted by Systems Manager deployments."
  value       = aws_instance.staging.id
}

output "instance_public_ip" {
  description = "Public egress address. The security group permits no inbound traffic."
  value       = aws_instance.staging.public_ip
}

output "github_actions_staging_role_arn" {
  description = "Role assumed by the main branch deployment job."
  value       = aws_iam_role.github_staging_deployer.arn
}

output "trusted_github_subject" {
  description = "Exact GitHub OIDC subject allowed to deploy staging."
  value       = local.github_subject
}

output "staging_data_lifecycle" {
  description = "Explicit reminder that database and uploaded files disappear with the instance."
  value       = "Disposable: terraform destroy deletes the encrypted root volume and all application data."
}
