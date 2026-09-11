output "ec2_instance_id" {
  description = "Production application instance managed through SSM."
  value       = aws_instance.application.id
}

output "ec2_private_ip" {
  description = "Private IPv4 address of the production application instance."
  value       = aws_instance.application.private_ip
}

output "ec2_public_ip" {
  description = "Ephemeral public IPv4 used only for outbound connectivity."
  value       = aws_instance.application.public_ip
}

output "iam_role_arn" {
  description = "ARN of the production application runtime role."
  value       = aws_iam_role.application.arn
}

output "instance_profile_name" {
  description = "IAM instance profile attached to the runtime instance."
  value       = aws_iam_instance_profile.application.name
}

output "application_security_group_id" {
  description = "Existing ingress-free application security group attached to EC2."
  value       = data.terraform_remote_state.foundation.outputs.application_security_group_id
}

output "runtime_subnet_id" {
  description = "Existing public subnet containing the first production runtime instance."
  value       = data.terraform_remote_state.foundation.outputs.public_subnet_ids[var.runtime_subnet_key]
}

output "public_api_url" {
  description = "Canonical HTTPS URL for the production API."
  value       = "https://${var.api_domain_name}"
}

output "load_balancer_dns_name" {
  description = "AWS DNS name of the production Application Load Balancer."
  value       = aws_lb.application.dns_name
}

output "load_balancer_security_group_id" {
  description = "Security group accepting public HTTP redirect and HTTPS traffic for the ALB."
  value       = aws_security_group.load_balancer.id
}

output "target_group_arn" {
  description = "Target group forwarding production API traffic to the EC2 application."
  value       = aws_lb_target_group.application.arn
}

output "api_certificate_arn" {
  description = "ACM certificate protecting the production API hostname."
  value       = aws_acm_certificate.api.arn
}
