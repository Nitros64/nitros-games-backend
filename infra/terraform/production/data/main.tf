locals {
  name_prefix = "${var.project_name}-${var.environment}"

  foundation_state = {
    bucket = "nitros-games-backend-tfstate-529601496188-eu-west-1"
    key    = "production/foundation/terraform.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "foundation" {
  backend = "s3"

  config = {
    bucket       = local.foundation_state.bucket
    key          = local.foundation_state.key
    region       = local.foundation_state.region
    encrypt      = true
    use_lockfile = true
  }
}

check "foundation_contract" {
  assert {
    condition     = data.terraform_remote_state.foundation.outputs.aws_region == var.aws_region
    error_message = "The production data root and foundation must use the same AWS region."
  }

  assert {
    condition     = length(values(data.terraform_remote_state.foundation.outputs.private_data_subnet_ids)) == 2
    error_message = "Production RDS requires exactly two private data subnets from foundation."
  }
}

resource "aws_db_subnet_group" "mysql" {
  name        = "${local.name_prefix}-mysql"
  description = "Private production data subnets for NitrosGames MySQL."
  subnet_ids  = values(data.terraform_remote_state.foundation.outputs.private_data_subnet_ids)

  tags = {
    Name = "${local.name_prefix}-mysql"
  }
}

resource "aws_db_parameter_group" "mysql84" {
  name        = "${local.name_prefix}-mysql84"
  description = "Minimal security and Unicode settings for NitrosGames MySQL 8.4."
  family      = "mysql8.4"

  parameter {
    name         = "character_set_server"
    value        = "utf8mb4"
    apply_method = "immediate"
  }

  parameter {
    name         = "collation_server"
    value        = "utf8mb4_unicode_ci"
    apply_method = "immediate"
  }

  parameter {
    name         = "require_secure_transport"
    value        = "1"
    apply_method = "immediate"
  }

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${local.name_prefix}-mysql84"
  }
}

resource "aws_db_instance" "mysql" {
  identifier = "${local.name_prefix}-mysql"

  engine         = "mysql"
  engine_version = var.mysql_engine_version
  instance_class = "db.t4g.micro"

  db_name  = "nitrosgames"
  username = "nitros_admin"
  port     = 3306

  manage_master_user_password = true

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"
  storage_encrypted     = true

  db_subnet_group_name   = aws_db_subnet_group.mysql.name
  vpc_security_group_ids = [data.terraform_remote_state.foundation.outputs.database_security_group_id]
  publicly_accessible    = false
  multi_az               = false

  parameter_group_name        = aws_db_parameter_group.mysql84.name
  auto_minor_version_upgrade  = false
  allow_major_version_upgrade = false
  apply_immediately           = false

  backup_retention_period  = 7
  backup_window            = "01:00-02:00"
  maintenance_window       = "sun:03:00-sun:04:00"
  copy_tags_to_snapshot    = true
  delete_automated_backups = false

  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = var.final_snapshot_identifier

  monitoring_interval          = 0
  performance_insights_enabled = false

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Name = "${local.name_prefix}-mysql"
  }
}
