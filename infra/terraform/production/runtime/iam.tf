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

data "aws_iam_policy_document" "rds_master_secret" {
  statement {
    sid    = "ReadTemporaryRdsMasterCredential"
    effect = "Allow"
    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetSecretValue"
    ]
    resources = [data.terraform_remote_state.data.outputs.master_secret_arn]
  }
}

resource "aws_iam_role_policy" "rds_master_secret" {
  name   = "read-temporary-rds-master-credential"
  role   = aws_iam_role.application.id
  policy = data.aws_iam_policy_document.rds_master_secret.json
}

resource "aws_iam_instance_profile" "application" {
  name = "${local.name_prefix}-application"
  role = aws_iam_role.application.name
}
