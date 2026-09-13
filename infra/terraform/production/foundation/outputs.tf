output "aws_region" {
  description = "AWS region containing the production network."
  value       = var.aws_region
}

output "public_hosted_zone_id" {
  description = "Route 53 public hosted zone for the official production domain."
  value       = aws_route53_zone.production.zone_id
}

output "public_name_servers" {
  description = "Authoritative name servers assigned to the production public hosted zone."
  value       = aws_route53_zone.production.name_servers
}

output "vpc_id" {
  description = "Production VPC ID."
  value       = aws_vpc.production.id
}

output "vpc_cidr" {
  description = "Production VPC IPv4 CIDR."
  value       = aws_vpc.production.cidr_block
}

output "availability_zones" {
  description = "Stable suffix-to-AZ mapping used by the production subnets."
  value       = local.availability_zones
}

output "public_subnet_ids" {
  description = "Public subnet IDs keyed by stable AZ suffix."
  value       = { for key, subnet in aws_subnet.public : key => subnet.id }
}

output "private_data_subnet_ids" {
  description = "Private data subnet IDs keyed by stable AZ suffix."
  value       = { for key, subnet in aws_subnet.private_data : key => subnet.id }
}

output "public_route_table_id" {
  description = "Route table shared by the public subnets."
  value       = aws_route_table.public.id
}

output "private_data_route_table_ids" {
  description = "Private data route table IDs keyed by stable AZ suffix."
  value       = { for key, route_table in aws_route_table.private_data : key => route_table.id }
}

output "application_security_group_id" {
  description = "Security group reserved for disposable production application compute."
  value       = aws_security_group.application.id
}

output "database_security_group_id" {
  description = "Security group reserved for production RDS."
  value       = aws_security_group.database.id
}

output "production_lifecycle_state_parameter_name" {
  description = "Authoritative non-secret ACTIVE or HIBERNATED production desired-state parameter."
  value       = aws_ssm_parameter.production_lifecycle_state.name
}

output "production_lifecycle_state_parameter_arn" {
  description = "ARN mutable only by the permanent production lifecycle role."
  value       = aws_ssm_parameter.production_lifecycle_state.arn
}

output "production_release_parameter_name" {
  description = "Non-secret last-successful-release metadata parameter."
  value       = aws_ssm_parameter.production_release_metadata.name
}

output "production_release_parameter_arn" {
  description = "ARN writable by the narrowly scoped production deployment role."
  value       = aws_ssm_parameter.production_release_metadata.arn
}

output "github_actions_production_lifecycle_role_arn" {
  description = "Persistent OIDC role used by protected production lifecycle workflows."
  value       = aws_iam_role.github_production_lifecycle.arn
}

output "production_lifecycle_trusted_subject" {
  description = "Exact GitHub Environment subject trusted by the lifecycle role."
  value       = local.github_environment_subject
}
