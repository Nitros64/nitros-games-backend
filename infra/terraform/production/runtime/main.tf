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

data "aws_ssm_parameter" "production_lifecycle_state" {
  name = data.terraform_remote_state.foundation.outputs.production_lifecycle_state_parameter_name
}

data "aws_resourcegroupstaggingapi_resources" "production_runtime" {
  resource_type_filters = [
    "acm:certificate",
    "ec2:instance",
    "ec2:security-group",
    "elasticloadbalancing:loadbalancer",
    "elasticloadbalancing:targetgroup",
    "iam:instance-profile",
    "iam:role"
  ]

  tag_filter {
    key    = "Project"
    values = [var.project_name]
  }

  tag_filter {
    key    = "Environment"
    values = [var.environment]
  }

  tag_filter {
    key    = "Component"
    values = ["production-runtime"]
  }
}

data "aws_route53_records" "production_runtime" {
  zone_id    = data.terraform_remote_state.foundation.outputs.public_hosted_zone_id
  name_regex = "^(api\\.${replace(var.api_domain_name, "api.", "")}|_.*\\.${replace(var.api_domain_name, "api.", "")})\\.?$"
}

locals {
  lifecycle_state = try(
    jsondecode(nonsensitive(data.aws_ssm_parameter.production_lifecycle_state.value)),
    {}
  )
  lifecycle_schema_version = try(local.lifecycle_state.schemaVersion, null)
  production_desired_state = try(local.lifecycle_state.desiredState, null)
  runtime_enabled          = local.production_desired_state == "ACTIVE"
  discovered_runtime_resources = (
    length(data.aws_resourcegroupstaggingapi_resources.production_runtime.resource_tag_mapping_list)
    + length(coalesce(data.aws_route53_records.production_runtime.resource_record_sets, []))
  )
}

check "production_lifecycle_state" {
  assert {
    condition     = local.lifecycle_schema_version == 1
    error_message = "The production lifecycle SSM parameter must use schemaVersion 1."
  }

  assert {
    condition     = contains(["ACTIVE", "HIBERNATED"], local.production_desired_state)
    error_message = "The production lifecycle SSM desiredState must be ACTIVE or HIBERNATED."
  }
}

check "runtime_hibernation_authorization" {
  assert {
    condition = (
      local.runtime_enabled
      || var.runtime_hibernation_authorized
      || local.discovered_runtime_resources == 0
    )
    error_message = "Runtime resources still exist while desiredState is HIBERNATED. Use the reviewed hibernation workflow; a normal plan cannot delete them."
  }
}

data "aws_partition" "current" {}

data "aws_ecr_repository" "application" {
  count = local.runtime_enabled ? 1 : 0

  name = var.ecr_repository_name
}

data "aws_ssm_parameter" "amazon_linux_2023" {
  count = local.runtime_enabled ? 1 : 0

  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

check "remote_state_contract" {
  assert {
    condition = !local.runtime_enabled || (
      data.terraform_remote_state.foundation.outputs.aws_region == var.aws_region
      && data.terraform_remote_state.data.outputs.db_port == 3306
    )
    error_message = "Production foundation, data and runtime must agree on region and MySQL port."
  }

  assert {
    condition = !local.runtime_enabled || contains(
      keys(data.terraform_remote_state.foundation.outputs.public_subnet_ids),
      var.runtime_subnet_key
    )
    error_message = "The selected runtime public subnet is not exported by production foundation."
  }

  assert {
    condition = !local.runtime_enabled || (
      length(data.terraform_remote_state.foundation.outputs.public_subnet_ids) == 2
      && data.terraform_remote_state.foundation.outputs.vpc_id != ""
      && data.terraform_remote_state.foundation.outputs.application_security_group_id != ""
      && data.terraform_remote_state.foundation.outputs.public_hosted_zone_id != ""
    )
    error_message = "Production foundation must expose the VPC, two public subnets, application security group and public hosted zone."
  }

  assert {
    condition = !local.runtime_enabled || (
      data.terraform_remote_state.data.outputs.application_db_secret_arn != ""
      && data.terraform_remote_state.data.outputs.host_images_bucket_arn != ""
      && data.terraform_remote_state.data.outputs.db_endpoint != ""
      && data.terraform_remote_state.data.outputs.db_name == "nitrosgames"
    )
    error_message = "Production data state must expose RDS, its application secret, and the host-images bucket."
  }
}

resource "aws_instance" "application" {
  count = local.runtime_enabled ? 1 : 0

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
