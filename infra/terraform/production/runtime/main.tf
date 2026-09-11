locals {
  name_prefix = "${var.project_name}-${var.environment}"

  state = {
    bucket = "nitros-games-backend-tfstate-529601496188-eu-west-1"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "foundation" {
  backend = "s3"

  config = {
    bucket       = local.state.bucket
    key          = "production/foundation/terraform.tfstate"
    region       = local.state.region
    encrypt      = true
    use_lockfile = true
  }
}

data "terraform_remote_state" "data" {
  backend = "s3"

  config = {
    bucket       = local.state.bucket
    key          = "production/data/terraform.tfstate"
    region       = local.state.region
    encrypt      = true
    use_lockfile = true
  }
}

data "aws_partition" "current" {}

data "aws_ecr_repository" "application" {
  count = var.runtime_enabled ? 1 : 0

  name = var.ecr_repository_name
}

data "aws_ssm_parameter" "amazon_linux_2023" {
  count = var.runtime_enabled ? 1 : 0

  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

check "remote_state_contract" {
  assert {
    condition = !var.runtime_enabled || (
      data.terraform_remote_state.foundation.outputs.aws_region == var.aws_region
      && data.terraform_remote_state.data.outputs.db_port == 3306
    )
    error_message = "Production foundation, data and runtime must agree on region and MySQL port."
  }

  assert {
    condition = !var.runtime_enabled || contains(
      keys(data.terraform_remote_state.foundation.outputs.public_subnet_ids),
      var.runtime_subnet_key
    )
    error_message = "The selected runtime public subnet is not exported by production foundation."
  }

  assert {
    condition = !var.runtime_enabled || (
      length(data.terraform_remote_state.foundation.outputs.public_subnet_ids) == 2
      && data.terraform_remote_state.foundation.outputs.vpc_id != ""
      && data.terraform_remote_state.foundation.outputs.application_security_group_id != ""
      && data.terraform_remote_state.foundation.outputs.public_hosted_zone_id != ""
    )
    error_message = "Production foundation must expose the VPC, two public subnets, application security group and public hosted zone."
  }

  assert {
    condition = !var.runtime_enabled || (
      data.terraform_remote_state.data.outputs.application_db_secret_arn != ""
      && data.terraform_remote_state.data.outputs.host_images_bucket_arn != ""
      && data.terraform_remote_state.data.outputs.db_endpoint != ""
      && data.terraform_remote_state.data.outputs.db_name == "nitrosgames"
    )
    error_message = "Production data state must expose RDS, its application secret, and the host-images bucket."
  }
}

resource "aws_instance" "application" {
  count = var.runtime_enabled ? 1 : 0

  ami                         = data.aws_ssm_parameter.amazon_linux_2023[0].value
  instance_type               = var.instance_type
  subnet_id                   = data.terraform_remote_state.foundation.outputs.public_subnet_ids[var.runtime_subnet_key]
  vpc_security_group_ids      = [data.terraform_remote_state.foundation.outputs.application_security_group_id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.application[0].name
  monitoring                  = false
  user_data_replace_on_change = true
  user_data = replace(
    templatefile("${path.module}/user-data.sh.tftpl", {
      docker_compose_version = var.docker_compose_version
      docker_compose_sha256  = var.docker_compose_sha256
    }),
    "\r\n",
    "\n"
  )

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
    instance_metadata_tags      = "disabled"
  }

  root_block_device {
    encrypted             = true
    delete_on_termination = true
    volume_type           = "gp3"
    volume_size           = var.root_volume_size_gib
  }

  tags = {
    Name = "${local.name_prefix}-application"
  }

  lifecycle {
    # The SSM parameter moves when AWS publishes a new AL2023 AMI. Replacement
    # must be a deliberate, reviewed action rather than incidental plan drift.
    ignore_changes = [ami, associate_public_ip_address]
  }
}
