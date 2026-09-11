data "aws_iam_policy_document" "instance_assume_role" {
  count = var.runtime_enabled ? 1 : 0

  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "application" {
  count = var.runtime_enabled ? 1 : 0

  name               = "${local.name_prefix}-application"
  description        = "Runtime identity for the single production application instance."
  assume_role_policy = data.aws_iam_policy_document.instance_assume_role[0].json
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  count = var.runtime_enabled ? 1 : 0

  role       = aws_iam_role.application[0].name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "ecr_pull" {
  count = var.runtime_enabled ? 1 : 0

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
    resources = [data.aws_ecr_repository.application[0].arn]
  }
}

resource "aws_iam_role_policy" "ecr_pull" {
  count = var.runtime_enabled ? 1 : 0

  name   = "pull-${var.ecr_repository_name}-images"
  role   = aws_iam_role.application[0].id
  policy = data.aws_iam_policy_document.ecr_pull[0].json
}

data "aws_iam_policy_document" "host_images" {
  count = var.runtime_enabled ? 1 : 0

  statement {
    sid    = "UseHostImageObjects"
    effect = "Allow"
    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject"
    ]
    resources = ["${data.terraform_remote_state.data.outputs.host_images_bucket_arn}/host-images/*"]
  }

  statement {
    sid       = "ListHostImagePrefix"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [data.terraform_remote_state.data.outputs.host_images_bucket_arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["host-images/", "host-images/*"]
    }
  }
}

resource "aws_iam_role_policy" "host_images" {
  count = var.runtime_enabled ? 1 : 0

  name   = "use-production-host-images"
  role   = aws_iam_role.application[0].id
  policy = data.aws_iam_policy_document.host_images[0].json
}

data "aws_iam_policy_document" "application_database_secret" {
  count = var.runtime_enabled ? 1 : 0

  statement {
    sid    = "ReadApplicationDatabaseCredential"
    effect = "Allow"
    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetSecretValue"
    ]
    resources = [data.terraform_remote_state.data.outputs.application_db_secret_arn]
  }
}

resource "aws_iam_role_policy" "application_database_secret" {
  count = var.runtime_enabled ? 1 : 0

  name   = "read-application-database-credential"
  role   = aws_iam_role.application[0].id
  policy = data.aws_iam_policy_document.application_database_secret[0].json
}

resource "aws_iam_instance_profile" "application" {
  count = var.runtime_enabled ? 1 : 0

  name = "${local.name_prefix}-application"
  role = aws_iam_role.application[0].name
}

data "aws_caller_identity" "current" {}

data "aws_iam_openid_connect_provider" "github_actions" {
  count = var.runtime_enabled ? 1 : 0

  arn = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/token.actions.githubusercontent.com"
}

data "aws_iam_policy_document" "github_production_assume_role" {
  count = var.runtime_enabled ? 1 : 0

  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [data.aws_iam_openid_connect_provider.github_actions[0].arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_owner}/${var.github_repository}:environment:${var.github_environment}"]
    }
  }
}

resource "aws_iam_role" "github_production_deployer" {
  count = var.runtime_enabled ? 1 : 0

  name               = "${local.name_prefix}-github-deployer"
  description        = "OIDC role for approved production deployments from GitHub Actions."
  assume_role_policy = data.aws_iam_policy_document.github_production_assume_role[0].json
}

data "aws_iam_policy_document" "github_production_deploy" {
  count = var.runtime_enabled ? 1 : 0

  statement {
    sid       = "VerifyImmutableApplicationImage"
    effect    = "Allow"
    actions   = ["ecr:DescribeImages"]
    resources = [data.aws_ecr_repository.application[0].arn]
  }

  statement {
    sid     = "RunProductionDeployment"
    effect  = "Allow"
    actions = ["ssm:SendCommand"]
    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${aws_instance.application[0].id}",
      "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::document/AWS-RunShellScript"
    ]
  }

  statement {
    sid    = "ReadProductionDeploymentStatus"
    effect = "Allow"
    actions = [
      "ssm:DescribeInstanceInformation",
      "ssm:GetCommandInvocation"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_production_deploy" {
  count = var.runtime_enabled ? 1 : 0

  name   = "deploy-existing-image-through-ssm"
  role   = aws_iam_role.github_production_deployer[0].id
  policy = data.aws_iam_policy_document.github_production_deploy[0].json
}
