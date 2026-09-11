output "ec2_instance_id" {
  description = "Production application instance managed through SSM."
  value       = try(aws_instance.application[0].id, null)
}

output "ec2_private_ip" {
  description = "Private IPv4 address of the production application instance."
  value       = try(aws_instance.application[0].private_ip, null)
}

output "ec2_public_ip" {
  description = "Ephemeral public IPv4 used only for outbound connectivity."
  value       = try(aws_instance.application[0].public_ip, null)
}

output "iam_role_arn" {
  description = "ARN of the production application runtime role."
  value       = try(aws_iam_role.application[0].arn, null)
}

output "instance_profile_name" {
  description = "IAM instance profile attached to the runtime instance."
  value       = try(aws_iam_instance_profile.application[0].name, null)
}

output "application_security_group_id" {
  description = "Existing ingress-free application security group attached to EC2."
  value       = var.runtime_enabled ? data.terraform_remote_state.foundation.outputs.application_security_group_id : null
}

output "runtime_subnet_id" {
  description = "Existing public subnet containing the first production runtime instance."
  value       = var.runtime_enabled ? data.terraform_remote_state.foundation.outputs.public_subnet_ids[var.runtime_subnet_key] : null
}

output "public_api_url" {
  description = "Canonical HTTPS URL for the production API."
  value       = var.runtime_enabled ? "https://${var.api_domain_name}" : null
}

output "load_balancer_dns_name" {
  description = "AWS DNS name of the production Application Load Balancer."
  value       = try(aws_lb.application[0].dns_name, null)
}

output "load_balancer_security_group_id" {
  description = "Security group accepting public HTTP redirect and HTTPS traffic for the ALB."
  value       = try(aws_security_group.load_balancer[0].id, null)
}

output "target_group_arn" {
  description = "Target group forwarding production API traffic to the EC2 application."
  value       = try(aws_lb_target_group.application[0].arn, null)
}

output "api_certificate_arn" {
  description = "ACM certificate protecting the production API hostname."
  value       = try(aws_acm_certificate.api[0].arn, null)
}

output "github_actions_production_role_arn" {
  description = "OIDC role used by the protected production GitHub Environment."
  value       = try(aws_iam_role.github_production_deployer[0].arn, null)
}

output "trusted_github_environment_subject" {
  description = "Exact GitHub OIDC subject allowed to assume the production deployment role."
  value       = var.runtime_enabled ? "repo:${var.github_owner}/${var.github_repository}:environment:${var.github_environment}" : null
}
