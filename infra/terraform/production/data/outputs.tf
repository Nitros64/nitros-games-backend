output "db_instance_identifier" {
  description = "Production RDS instance identifier."
  value       = one(aws_db_instance.mysql[*].identifier)
}

output "db_endpoint" {
  description = "Production RDS endpoint including its port."
  value       = one(aws_db_instance.mysql[*].endpoint)
}

output "db_port" {
  description = "Production MySQL port."
  value       = one(aws_db_instance.mysql[*].port)
}

output "db_name" {
  description = "Initial database managed by Flyway at application startup."
  value       = one(aws_db_instance.mysql[*].db_name)
}

output "master_secret_arn" {
  description = "ARN of the master credential secret generated and managed by RDS."
  value       = try(one(aws_db_instance.mysql[*].master_user_secret[0].secret_arn), null)
}

output "hibernation_snapshot_identifier" {
  description = "Canonical manual RDS snapshot retained for production restoration."
  value       = aws_db_snapshot.hibernation.db_snapshot_identifier
}

output "hibernation_snapshot_arn" {
  description = "ARN of the canonical encrypted production hibernation snapshot."
  value       = aws_db_snapshot.hibernation.db_snapshot_arn
}

output "hibernation_snapshot_status" {
  description = "Current provider-observed status of the canonical hibernation snapshot."
  value       = aws_db_snapshot.hibernation.status
}

output "application_db_secret_arn" {
  description = "ARN of the metadata-only secret reserved for the dedicated application database credential."
  value       = aws_secretsmanager_secret.application_database.arn
}

output "db_subnet_group_name" {
  description = "DB subnet group backed by the two private production data subnets."
  value       = aws_db_subnet_group.mysql.name
}

output "parameter_group_name" {
  description = "Custom MySQL 8.4 parameter group name."
  value       = aws_db_parameter_group.mysql84.name
}

output "host_images_bucket_name" {
  description = "Private S3 bucket for persistent production host images."
  value       = local.host_images_bucket_name
}

output "host_images_bucket_arn" {
  description = "ARN of the private production host-images bucket."
  value       = aws_s3_bucket.host_images.arn
}
