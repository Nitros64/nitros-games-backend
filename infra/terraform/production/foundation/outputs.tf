output "aws_region" {
  description = "AWS region containing the production network."
  value       = var.aws_region
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
