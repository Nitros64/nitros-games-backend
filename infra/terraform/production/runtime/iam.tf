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

resource "aws_iam_role" "application" {
  name               = "${local.name_prefix}-application"
  description        = "Runtime identity for the single production application instance."
  assume_role_policy = data.aws_iam_policy_document.instance_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.application.name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "ecr_pull" {
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
    resources = [data.aws_ecr_repository.application.arn]
  }
}

resource "aws_iam_role_policy" "ecr_pull" {
  name   = "pull-${var.ecr_repository_name}-images"
  role   = aws_iam_role.application.id
  policy = data.aws_iam_policy_document.ecr_pull.json
}

data "aws_iam_policy_document" "host_images" {
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
  name   = "use-production-host-images"
  role   = aws_iam_role.application.id
  policy = data.aws_iam_policy_document.host_images.json
}

data "aws_iam_policy_document" "application_database_secret" {
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
  name   = "read-application-database-credential"
  role   = aws_iam_role.application.id
  policy = data.aws_iam_policy_document.application_database_secret.json
}

resource "aws_iam_instance_profile" "application" {
  name = "${local.name_prefix}-application"
  role = aws_iam_role.application.name
}

data "aws_caller_identity" "current" {}

data "aws_iam_openid_connect_provider" "github_actions" {
  arn = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/token.actions.githubusercontent.com"
}

data "aws_iam_policy_document" "github_production_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [data.aws_iam_openid_connect_provider.github_actions.arn]
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
  name               = "${local.name_prefix}-github-deployer"
  description        = "OIDC role for approved production deployments from GitHub Actions."
  assume_role_policy = data.aws_iam_policy_document.github_production_assume_role.json
}

data "aws_iam_policy_document" "github_production_deploy" {
  statement {
    sid       = "VerifyImmutableApplicationImage"
    effect    = "Allow"
    actions   = ["ecr:DescribeImages"]
    resources = [data.aws_ecr_repository.application.arn]
  }

  statement {
    sid     = "RunProductionDeployment"
    effect  = "Allow"
    actions = ["ssm:SendCommand"]
    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${aws_instance.application.id}",
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
  name   = "deploy-existing-image-through-ssm"
  role   = aws_iam_role.github_production_deployer.id
  policy = data.aws_iam_policy_document.github_production_deploy.json
}
