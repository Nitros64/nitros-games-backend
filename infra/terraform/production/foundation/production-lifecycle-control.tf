data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

locals {
  lifecycle_state_parameter_name   = "/${var.project_name}/${var.environment}/lifecycle/state"
  lifecycle_release_parameter_name = "/${var.project_name}/${var.environment}/lifecycle/release"
  lifecycle_state_parameter_arn    = "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter${local.lifecycle_state_parameter_name}"
  lifecycle_release_parameter_arn  = "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter${local.lifecycle_release_parameter_name}"
  terraform_state_bucket_name      = "${var.project_name}-tfstate-${data.aws_caller_identity.current.account_id}-${var.aws_region}"
  terraform_state_bucket_arn       = "arn:${data.aws_partition.current.partition}:s3:::${local.terraform_state_bucket_name}"
  ecr_repository_arn               = "arn:${data.aws_partition.current.partition}:ecr:${var.aws_region}:${data.aws_caller_identity.current.account_id}:repository/${var.project_name}"
  database_instance_arn            = "arn:${data.aws_partition.current.partition}:rds:${var.aws_region}:${data.aws_caller_identity.current.account_id}:db:${local.name_prefix}-mysql"
  application_secret_arn_pattern   = "arn:${data.aws_partition.current.partition}:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:${var.project_name}/${var.environment}/database/application-*"
  application_role_arn             = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:role/${local.name_prefix}-application"
  deployment_role_arn              = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:role/${local.name_prefix}-github-deployer"
  application_profile_arn          = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:instance-profile/${local.name_prefix}-application"
  application_security_group_arn   = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:security-group/${aws_security_group.application.id}"
  amazon_linux_image_arn           = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}::image/ami-*"
  runtime_subnet_arn               = aws_subnet.public["a"].arn
  runtime_security_group_arn       = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:security-group/*"
  runtime_security_group_rule_arn  = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:security-group-rule/*"
  runtime_instance_arn             = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/*"
  runtime_volume_arn               = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:volume/*"
  runtime_network_interface_arn    = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:network-interface/*"
  runtime_public_endpoint_name     = "${var.project_name}-prod-api"
  runtime_load_balancer_arn        = "arn:${data.aws_partition.current.partition}:elasticloadbalancing:${var.aws_region}:${data.aws_caller_identity.current.account_id}:loadbalancer/app/${local.runtime_public_endpoint_name}/*"
  runtime_listener_arn             = "arn:${data.aws_partition.current.partition}:elasticloadbalancing:${var.aws_region}:${data.aws_caller_identity.current.account_id}:listener/app/${local.runtime_public_endpoint_name}/*/*"
  runtime_target_group_arn         = "arn:${data.aws_partition.current.partition}:elasticloadbalancing:${var.aws_region}:${data.aws_caller_identity.current.account_id}:targetgroup/${local.runtime_public_endpoint_name}/*"
  runtime_certificate_arn          = "arn:${data.aws_partition.current.partition}:acm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:certificate/*"
  github_oidc_provider_arn         = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/token.actions.githubusercontent.com"
  github_environment_subject       = "repo:Nitros64/${var.project_name}:environment:production"

  initial_lifecycle_state = jsonencode({
    schemaVersion     = 1
    desiredState      = "HIBERNATED"
    lastTransitionRun = null
    updatedAt         = "2026-09-13T00:00:00Z"
  })

  initial_release_metadata = jsonencode({
    schemaVersion  = 1
    applicationSha = "b6d95eaece3be14c5ea9f5b71fdf4aa41b7719ae"
    imageDigest    = "sha256:f9823528ca42d686bf6939a593352373f60384dce2e5dea4cac246ef1ffdd606"
    updatedAt      = "2026-09-11T00:00:00Z"
  })
}

resource "aws_ssm_parameter" "production_lifecycle_state" {
  name        = local.lifecycle_state_parameter_name
  description = "Non-secret authoritative ACTIVE or HIBERNATED production desired state."
  type        = "String"
  value       = local.initial_lifecycle_state

  lifecycle {
    prevent_destroy = true
    ignore_changes  = [value]
  }

  tags = {
    Name    = local.lifecycle_state_parameter_name
    Purpose = "Production lifecycle desired state"
  }
}

resource "aws_ssm_parameter" "production_release_metadata" {
  name        = local.lifecycle_release_parameter_name
  description = "Non-secret metadata for the last successful immutable production release."
  type        = "String"
  value       = local.initial_release_metadata

  lifecycle {
    prevent_destroy = true
    ignore_changes  = [value]
  }

  tags = {
    Name    = local.lifecycle_release_parameter_name
    Purpose = "Production release metadata"
  }
}

data "aws_iam_openid_connect_provider" "github_actions" {
  arn = local.github_oidc_provider_arn
}

data "aws_iam_policy_document" "github_production_lifecycle_assume_role" {
  statement {
    sid     = "GitHubProductionEnvironment"
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
      values   = [local.github_environment_subject]
    }
  }
}

resource "aws_iam_role" "github_production_lifecycle" {
  name                 = "${local.name_prefix}-github-lifecycle"
  description          = "Persistent OIDC control plane for reviewed production lifecycle operations."
  assume_role_policy   = data.aws_iam_policy_document.github_production_lifecycle_assume_role.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_production_lifecycle_state" {
  statement {
    sid       = "ListProductionTerraformState"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [local.terraform_state_bucket_arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["production/*/terraform.tfstate*"]
    }
  }

  statement {
    sid     = "ReadProductionTerraformState"
    effect  = "Allow"
    actions = ["s3:GetObject"]
    resources = [
      "${local.terraform_state_bucket_arn}/production/foundation/terraform.tfstate",
      "${local.terraform_state_bucket_arn}/production/data/terraform.tfstate",
      "${local.terraform_state_bucket_arn}/production/data/terraform.tfstate.tflock",
      "${local.terraform_state_bucket_arn}/production/identity/terraform.tfstate",
      "${local.terraform_state_bucket_arn}/production/runtime/terraform.tfstate",
      "${local.terraform_state_bucket_arn}/production/runtime/terraform.tfstate.tflock"
    ]
  }

  statement {
    sid     = "WriteDisposableProductionTerraformState"
    effect  = "Allow"
    actions = ["s3:PutObject"]
    resources = [
      "${local.terraform_state_bucket_arn}/production/data/terraform.tfstate",
      "${local.terraform_state_bucket_arn}/production/data/terraform.tfstate.tflock",
      "${local.terraform_state_bucket_arn}/production/runtime/terraform.tfstate",
      "${local.terraform_state_bucket_arn}/production/runtime/terraform.tfstate.tflock"
    ]
  }

  statement {
    sid     = "ReleaseDisposableProductionTerraformLocks"
    effect  = "Allow"
    actions = ["s3:DeleteObject"]
    resources = [
      "${local.terraform_state_bucket_arn}/production/data/terraform.tfstate.tflock",
      "${local.terraform_state_bucket_arn}/production/runtime/terraform.tfstate.tflock"
    ]
  }

  statement {
    sid    = "ReadProductionLifecycleMetadata"
    effect = "Allow"
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters"
    ]
    resources = [
      local.lifecycle_state_parameter_arn,
      local.lifecycle_release_parameter_arn
    ]
  }

  statement {
    sid       = "TransitionProductionLifecycleState"
    effect    = "Allow"
    actions   = ["ssm:PutParameter"]
    resources = [local.lifecycle_state_parameter_arn]
  }

  statement {
    sid       = "RecordRestoredProductionRelease"
    effect    = "Allow"
    actions   = ["ssm:PutParameter"]
    resources = [local.lifecycle_release_parameter_arn]
  }
}

resource "aws_iam_policy" "github_production_lifecycle_state" {
  name        = "${local.name_prefix}-github-lifecycle-state"
  description = "Persistent state and release metadata access for production lifecycle operations."
  policy      = data.aws_iam_policy_document.github_production_lifecycle_state.json
}

resource "aws_iam_role_policy_attachment" "github_production_lifecycle_state" {
  role       = aws_iam_role.github_production_lifecycle.name
  policy_arn = aws_iam_policy.github_production_lifecycle_state.arn
}

data "aws_iam_policy_document" "github_production_lifecycle_data" {
  statement {
    sid    = "InspectProductionData"
    effect = "Allow"
    actions = [
      "rds:DescribeDBEngineVersions",
      "rds:DescribeDBInstances",
      "rds:DescribeDBParameterGroups",
      "rds:DescribeDBParameters",
      "rds:DescribeDBSnapshots",
      "rds:DescribeDBSubnetGroups",
      "rds:ListTagsForResource"
    ]
    resources = ["*"]
  }

  statement {
    sid     = "InspectProductionHibernationSnapshotAttributes"
    effect  = "Allow"
    actions = ["rds:DescribeDBSnapshotAttributes"]
    resources = [
      "arn:${data.aws_partition.current.partition}:rds:${var.aws_region}:${data.aws_caller_identity.current.account_id}:snapshot:${var.project_name}-${var.environment}-hibernation-*"
    ]
  }

  statement {
    sid    = "OperateProductionDatabase"
    effect = "Allow"
    actions = [
      "rds:DeleteDBInstance",
      "rds:ModifyDBInstance",
      "rds:StartDBInstance",
      "rds:StopDBInstance"
    ]
    resources = [local.database_instance_arn]
  }

  statement {
    sid    = "CreateProductionDatabaseFromRetainedSnapshot"
    effect = "Allow"
    actions = [
      "rds:CreateDBInstance",
      "rds:RestoreDBInstanceFromDBSnapshot"
    ]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

  }

  statement {
    sid    = "CreateAndTagProductionHibernationSnapshots"
    effect = "Allow"
    actions = [
      "rds:AddTagsToResource",
      "rds:CreateDBSnapshot"
    ]
    resources = [
      local.database_instance_arn,
      "arn:${data.aws_partition.current.partition}:rds:${var.aws_region}:${data.aws_caller_identity.current.account_id}:snapshot:${var.project_name}-${var.environment}-hibernation-*"
    ]
  }

  statement {
    sid    = "InspectPersistentApplicationSecret"
    effect = "Allow"
    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetResourcePolicy",
      "secretsmanager:ListSecretVersionIds"
    ]
    resources = [local.application_secret_arn_pattern]
  }

  statement {
    sid    = "InspectPersistentHostImageBucket"
    effect = "Allow"
    actions = [
      "s3:GetBucketLocation",
      "s3:GetAccelerateConfiguration",
      "s3:GetBucketAcl",
      "s3:GetBucketCORS",
      "s3:GetLifecycleConfiguration",
      "s3:GetBucketLogging",
      "s3:GetBucketObjectLockConfiguration",
      "s3:GetBucketOwnershipControls",
      "s3:GetBucketPolicy",
      "s3:GetBucketPolicyStatus",
      "s3:GetBucketPublicAccessBlock",
      "s3:GetBucketRequestPayment",
      "s3:GetBucketTagging",
      "s3:GetBucketVersioning",
      "s3:GetBucketWebsite",
      "s3:GetEncryptionConfiguration",
      "s3:GetReplicationConfiguration",
      "s3:ListBucket"
    ]
    resources = ["arn:${data.aws_partition.current.partition}:s3:::nitros-games-prod-host-images-${data.aws_caller_identity.current.account_id}-${var.aws_region}"]
  }
}

resource "aws_iam_policy" "github_production_lifecycle_data" {
  name        = "${local.name_prefix}-github-lifecycle-data"
  description = "RDS and retained data access for production lifecycle operations."
  policy      = data.aws_iam_policy_document.github_production_lifecycle_data.json
}

resource "aws_iam_role_policy_attachment" "github_production_lifecycle_data" {
  role       = aws_iam_role.github_production_lifecycle.name
  policy_arn = aws_iam_policy.github_production_lifecycle_data.arn
}

data "aws_iam_policy_document" "github_production_lifecycle_runtime_compute" {
  statement {
    sid    = "InspectProductionRuntime"
    effect = "Allow"
    actions = [
      "ec2:DescribeAvailabilityZones",
      "ec2:DescribeImages",
      "ec2:DescribeInstanceAttribute",
      "ec2:DescribeInstanceCreditSpecifications",
      "ec2:DescribeInstanceStatus",
      "ec2:DescribeInstanceTypes",
      "ec2:DescribeInstances",
      "ec2:DescribeNetworkInterfaces",
      "ec2:DescribeSecurityGroupRules",
      "ec2:DescribeSecurityGroups",
      "ec2:DescribeSubnets",
      "ec2:DescribeTags",
      "ec2:DescribeVolumes",
      "ec2:DescribeVpcs",
      "iam:GetInstanceProfile",
      "iam:GetOpenIDConnectProvider",
      "iam:GetRole",
      "iam:GetRolePolicy",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfiles",
      "iam:ListInstanceProfilesForRole",
      "iam:ListRolePolicies",
      "iam:ListRoles",
      "ssm:DescribeInstanceInformation",
      "tag:GetResources"
    ]
    resources = ["*"]
  }

  statement {
    sid       = "RunProductionInstanceFromAmazonImage"
    effect    = "Allow"
    actions   = ["ec2:RunInstances"]
    resources = [local.amazon_linux_image_arn]

    condition {
      test     = "StringEquals"
      variable = "ec2:Owner"
      values   = ["amazon"]
    }
  }

  statement {
    sid     = "UseProductionRuntimeLaunchNetwork"
    effect  = "Allow"
    actions = ["ec2:RunInstances"]
    resources = [
      local.application_security_group_arn,
      local.runtime_network_interface_arn,
      local.runtime_subnet_arn
    ]
  }

  statement {
    sid     = "CreateTaggedProductionInstanceAndVolume"
    effect  = "Allow"
    actions = ["ec2:RunInstances"]
    resources = [
      local.runtime_instance_arn,
      local.runtime_volume_arn
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Component"
      values   = ["production-runtime"]
    }
  }

  statement {
    sid       = "UseProductionVpcForRuntimeSecurityGroup"
    effect    = "Allow"
    actions   = ["ec2:CreateSecurityGroup"]
    resources = [aws_vpc.production.arn]
  }

  statement {
    sid       = "CreateTaggedProductionRuntimeSecurityGroup"
    effect    = "Allow"
    actions   = ["ec2:CreateSecurityGroup"]
    resources = [local.runtime_security_group_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Component"
      values   = ["production-runtime"]
    }
  }

  statement {
    sid     = "TagNewProductionRuntimeResources"
    effect  = "Allow"
    actions = ["ec2:CreateTags"]
    resources = [
      local.runtime_instance_arn,
      local.runtime_network_interface_arn,
      local.runtime_security_group_arn,
      local.runtime_security_group_rule_arn,
      local.runtime_volume_arn
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Component"
      values   = ["production-runtime"]
    }

    condition {
      test     = "StringEquals"
      variable = "ec2:CreateAction"
      values = [
        "AuthorizeSecurityGroupEgress",
        "AuthorizeSecurityGroupIngress",
        "CreateSecurityGroup",
        "RunInstances"
      ]
    }
  }

  statement {
    sid    = "ManageTaggedProductionCompute"
    effect = "Allow"
    actions = [
      "ec2:DeleteSecurityGroup",
      "ec2:StartInstances",
      "ec2:StopInstances",
      "ec2:TerminateInstances"
    ]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Component"
      values   = ["production-runtime"]
    }
  }

  statement {
    sid    = "ManagePersistentApplicationIngress"
    effect = "Allow"
    actions = [
      "ec2:AuthorizeSecurityGroupIngress",
      "ec2:RevokeSecurityGroupIngress"
    ]
    resources = [local.application_security_group_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "ManageTaggedRuntimeSecurityGroupRules"
    effect = "Allow"
    actions = [
      "ec2:AuthorizeSecurityGroupEgress",
      "ec2:AuthorizeSecurityGroupIngress",
      "ec2:RevokeSecurityGroupEgress",
      "ec2:RevokeSecurityGroupIngress"
    ]
    resources = [local.runtime_security_group_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Component"
      values   = ["production-runtime"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "ManageProductionRuntimeRoles"
    effect = "Allow"
    actions = [
      "iam:AttachRolePolicy",
      "iam:CreateRole",
      "iam:DeleteRole",
      "iam:DeleteRolePolicy",
      "iam:DetachRolePolicy",
      "iam:PutRolePolicy",
      "iam:TagRole",
      "iam:UntagRole",
      "iam:UpdateAssumeRolePolicy"
    ]
    resources = [local.application_role_arn, local.deployment_role_arn]
  }

  statement {
    sid    = "ManageProductionApplicationInstanceProfile"
    effect = "Allow"
    actions = [
      "iam:AddRoleToInstanceProfile",
      "iam:CreateInstanceProfile",
      "iam:DeleteInstanceProfile",
      "iam:RemoveRoleFromInstanceProfile",
      "iam:TagInstanceProfile",
      "iam:UntagInstanceProfile"
    ]
    resources = [local.application_profile_arn]
  }

  statement {
    sid       = "PassProductionApplicationRoleToEC2"
    effect    = "Allow"
    actions   = ["iam:PassRole"]
    resources = [local.application_role_arn]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ec2.amazonaws.com"]
    }
  }

  statement {
    sid       = "ReadAmazonLinuxImageParameter"
    effect    = "Allow"
    actions   = ["ssm:GetParameter"]
    resources = ["arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::parameter/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"]
  }

  statement {
    sid       = "VerifyImmutableProductionImage"
    effect    = "Allow"
    actions   = ["ecr:DescribeImages", "ecr:DescribeRepositories", "ecr:ListTagsForResource"]
    resources = [local.ecr_repository_arn]
  }

  statement {
    sid       = "RunCommandsOnTaggedProductionApplication"
    effect    = "Allow"
    actions   = ["ssm:SendCommand"]
    resources = ["arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/*"]

    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/Component"
      values   = ["production-runtime"]
    }
  }

  statement {
    sid       = "UseRunShellScriptDocument"
    effect    = "Allow"
    actions   = ["ssm:SendCommand"]
    resources = ["arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::document/AWS-RunShellScript"]
  }

  statement {
    sid       = "ReadLifecycleCommandResults"
    effect    = "Allow"
    actions   = ["ssm:GetCommandInvocation"]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "github_production_lifecycle_runtime_compute" {
  name        = "${local.name_prefix}-github-lifecycle-runtime-compute"
  description = "EC2, IAM, ECR and SSM access for production runtime lifecycle operations."
  policy      = data.aws_iam_policy_document.github_production_lifecycle_runtime_compute.json
}

resource "aws_iam_role_policy_attachment" "github_production_lifecycle_runtime_compute" {
  role       = aws_iam_role.github_production_lifecycle.name
  policy_arn = aws_iam_policy.github_production_lifecycle_runtime_compute.arn
}

data "aws_iam_policy_document" "github_production_lifecycle_runtime_edge" {
  statement {
    sid    = "InspectProductionRuntimeEdge"
    effect = "Allow"
    actions = [
      "acm:ListCertificates",
      "elasticloadbalancing:DescribeListeners",
      "elasticloadbalancing:DescribeListenerAttributes",
      "elasticloadbalancing:DescribeLoadBalancerAttributes",
      "elasticloadbalancing:DescribeLoadBalancers",
      "elasticloadbalancing:DescribeRules",
      "elasticloadbalancing:DescribeTags",
      "elasticloadbalancing:DescribeTargetGroupAttributes",
      "elasticloadbalancing:DescribeTargetGroups",
      "elasticloadbalancing:DescribeTargetHealth",
      "route53:GetChange",
      "route53:ListResourceRecordSets"
    ]
    resources = ["*"]
  }

  statement {
    sid       = "ReadProductionHostedZone"
    effect    = "Allow"
    actions   = ["route53:GetHostedZone"]
    resources = [aws_route53_zone.production.arn]
  }

  statement {
    sid    = "CreateProductionRuntimeSecurityGroupRules"
    effect = "Allow"
    actions = [
      "ec2:AuthorizeSecurityGroupEgress",
      "ec2:AuthorizeSecurityGroupIngress"
    ]
    resources = [local.runtime_security_group_rule_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "CreateTaggedProductionLoadBalancer"
    effect = "Allow"
    actions = [
      "elasticloadbalancing:CreateListener",
      "elasticloadbalancing:CreateLoadBalancer"
    ]
    resources = [local.runtime_load_balancer_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Component"
      values   = ["production-runtime"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid       = "CreateTaggedProductionTargetGroup"
    effect    = "Allow"
    actions   = ["elasticloadbalancing:CreateTargetGroup"]
    resources = [local.runtime_target_group_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Component"
      values   = ["production-runtime"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid     = "TagNewProductionLoadBalancingResources"
    effect  = "Allow"
    actions = ["elasticloadbalancing:AddTags"]
    resources = [
      local.runtime_listener_arn,
      local.runtime_load_balancer_arn,
      local.runtime_target_group_arn
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Component"
      values   = ["production-runtime"]
    }

    condition {
      test     = "StringEquals"
      variable = "elasticloadbalancing:CreateAction"
      values   = ["CreateListener", "CreateLoadBalancer", "CreateTargetGroup"]
    }
  }

  statement {
    sid    = "ManageTaggedProductionLoadBalancing"
    effect = "Allow"
    actions = [
      "elasticloadbalancing:DeleteListener",
      "elasticloadbalancing:DeleteLoadBalancer",
      "elasticloadbalancing:DeleteTargetGroup",
      "elasticloadbalancing:DeregisterTargets",
      "elasticloadbalancing:ModifyLoadBalancerAttributes",
      "elasticloadbalancing:ModifyTargetGroup",
      "elasticloadbalancing:ModifyTargetGroupAttributes",
      "elasticloadbalancing:RegisterTargets",
      "elasticloadbalancing:SetSecurityGroups",
      "elasticloadbalancing:SetSubnets"
    ]
    resources = [
      local.runtime_listener_arn,
      local.runtime_load_balancer_arn,
      local.runtime_target_group_arn
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Component"
      values   = ["production-runtime"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid       = "RequestProductionApiCertificate"
    effect    = "Allow"
    actions   = ["acm:RequestCertificate"]
    resources = ["*"]

    condition {
      test     = "ForAllValues:StringEquals"
      variable = "acm:DomainNames"
      values   = ["api.${var.domain_name}"]
    }

    condition {
      test     = "StringEquals"
      variable = "acm:ValidationMethod"
      values   = ["DNS"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Component"
      values   = ["production-runtime"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "InspectAndDeleteTaggedProductionApiCertificate"
    effect = "Allow"
    actions = [
      "acm:DeleteCertificate",
      "acm:DescribeCertificate",
      "acm:ListTagsForCertificate"
    ]
    resources = [local.runtime_certificate_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Component"
      values   = ["production-runtime"]
    }
  }

  statement {
    sid       = "TagNewProductionApiCertificate"
    effect    = "Allow"
    actions   = ["acm:AddTagsToCertificate"]
    resources = [local.runtime_certificate_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Environment"
      values   = [var.environment]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Component"
      values   = ["production-runtime"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid       = "ManageProductionApiDnsRecords"
    effect    = "Allow"
    actions   = ["route53:ChangeResourceRecordSets"]
    resources = [aws_route53_zone.production.arn]

    condition {
      test     = "ForAllValues:StringLike"
      variable = "route53:ChangeResourceRecordSetsNormalizedRecordNames"
      values   = ["api.${var.domain_name}", "_*.api.${var.domain_name}"]
    }
  }
}

resource "aws_iam_policy" "github_production_lifecycle_runtime_edge" {
  name        = "${local.name_prefix}-github-lifecycle-runtime-edge"
  description = "ALB, ACM and Route53 access for production runtime lifecycle operations."
  policy      = data.aws_iam_policy_document.github_production_lifecycle_runtime_edge.json
}

resource "aws_iam_role_policy_attachment" "github_production_lifecycle_runtime_edge" {
  role       = aws_iam_role.github_production_lifecycle.name
  policy_arn = aws_iam_policy.github_production_lifecycle_runtime_edge.arn
}
