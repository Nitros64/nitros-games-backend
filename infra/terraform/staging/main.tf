data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ssm_parameter" "amazon_linux_2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
  github_subject = format(
    "repo:%s/%s:ref:refs/heads/%s",
    var.github_owner,
    var.github_repository,
    var.github_branch
  )
  ecr_repository_arn = format(
    "arn:%s:ecr:%s:%s:repository/%s",
    data.aws_partition.current.partition,
    var.aws_region,
    data.aws_caller_identity.current.account_id,
    var.ecr_repository_name
  )
}

resource "aws_vpc" "staging" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${local.name_prefix}-vpc"
  }
}

resource "aws_internet_gateway" "staging" {
  vpc_id = aws_vpc.staging.id

  tags = {
    Name = "${local.name_prefix}-igw"
  }
}

resource "aws_subnet" "staging" {
  vpc_id                  = aws_vpc.staging.id
  cidr_block              = var.subnet_cidr
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name = "${local.name_prefix}-public"
  }
}

resource "aws_route_table" "staging" {
  vpc_id = aws_vpc.staging.id

  tags = {
    Name = "${local.name_prefix}-public"
  }
}

resource "aws_route" "internet" {
  route_table_id         = aws_route_table.staging.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.staging.id
}

resource "aws_route_table_association" "staging" {
  subnet_id      = aws_subnet.staging.id
  route_table_id = aws_route_table.staging.id
}

resource "aws_security_group" "staging" {
  name        = "${local.name_prefix}-instance"
  description = "No inbound access; administration and smoke tests use AWS Systems Manager."
  vpc_id      = aws_vpc.staging.id

  tags = {
    Name = "${local.name_prefix}-instance"
  }
}

resource "aws_vpc_security_group_egress_rule" "internet" {
  security_group_id = aws_security_group.staging.id
  description       = "Allow package, ECR, SSM and application outbound traffic."
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

data "aws_iam_policy_document" "instance_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "instance" {
  name               = "${local.name_prefix}-instance"
  description        = "Runtime identity for the disposable staging EC2 instance."
  assume_role_policy = data.aws_iam_policy_document.instance_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.instance.name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "instance_ecr_pull" {
  statement {
    sid       = "AuthenticateToEcr"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid    = "PullApplicationImage"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer"
    ]
    resources = [local.ecr_repository_arn]
  }
}

resource "aws_iam_role_policy" "instance_ecr_pull" {
  name   = "pull-${var.ecr_repository_name}-images"
  role   = aws_iam_role.instance.id
  policy = data.aws_iam_policy_document.instance_ecr_pull.json
}

resource "aws_iam_instance_profile" "staging" {
  name = "${local.name_prefix}-instance"
  role = aws_iam_role.instance.name
}

resource "aws_instance" "staging" {
  ami                         = data.aws_ssm_parameter.amazon_linux_2023.value
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.staging.id
  vpc_security_group_ids      = [aws_security_group.staging.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.staging.name
  monitoring                  = false
  user_data_replace_on_change = true
  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    docker_compose_version = var.docker_compose_version
    docker_compose_sha256  = var.docker_compose_sha256
  })

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
    instance_metadata_tags      = "disabled"
  }

  root_block_device {
    encrypted             = true
    delete_on_termination = true
    volume_type           = "gp3"
    volume_size           = var.root_volume_size_gib
  }

  tags = {
    Name = local.name_prefix
  }

  depends_on = [aws_route.internet]
}

data "aws_iam_policy_document" "github_deployer_assume_role" {
  statement {
    sid     = "GitHubActionsMainBranch"
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [var.github_oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [local.github_subject]
    }
  }
}

resource "aws_iam_role" "github_staging_deployer" {
  name                 = "${local.name_prefix}-github-deployer"
  description          = "Allows the main branch workflow to deploy staging through SSM."
  assume_role_policy   = data.aws_iam_policy_document.github_deployer_assume_role.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_staging_deploy" {
  statement {
    sid     = "RunStagingDeployment"
    effect  = "Allow"
    actions = ["ssm:SendCommand"]
    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${aws_instance.staging.id}",
      "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::document/AWS-RunShellScript"
    ]
  }

  statement {
    sid    = "ReadDeploymentResult"
    effect = "Allow"
    actions = [
      "ssm:GetCommandInvocation",
      "ssm:ListCommandInvocations"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_staging_deploy" {
  name   = "deploy-${local.name_prefix}"
  role   = aws_iam_role.github_staging_deployer.id
  policy = data.aws_iam_policy_document.github_staging_deploy.json
}
