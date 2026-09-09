data "aws_caller_identity" "current" {}

locals {
  host_images_bucket_name = format(
    "nitros-games-prod-host-images-%s-%s",
    data.aws_caller_identity.current.account_id,
    var.aws_region
  )
}

check "host_images_bucket_name" {
  assert {
    condition = (
      length(local.host_images_bucket_name) >= 3
      && length(local.host_images_bucket_name) <= 63
      && can(regex("^[a-z0-9][a-z0-9.-]*[a-z0-9]$", local.host_images_bucket_name))
    )
    error_message = "The deterministic host-images bucket name must satisfy S3 naming rules."
  }
}

resource "aws_s3_bucket" "host_images" {
  bucket        = local.host_images_bucket_name
  force_destroy = false

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Name    = local.host_images_bucket_name
    Purpose = "Persistent production host images"
  }
}

resource "aws_s3_bucket_public_access_block" "host_images" {
  bucket = aws_s3_bucket.host_images.id

  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "host_images" {
  bucket = aws_s3_bucket.host_images.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "host_images" {
  bucket = aws_s3_bucket.host_images.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "host_images" {
  bucket = aws_s3_bucket.host_images.id

  versioning_configuration {
    status = "Enabled"
  }
}

data "aws_iam_policy_document" "host_images" {
  statement {
    sid    = "DenyInsecureTransport"
    effect = "Deny"

    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.host_images.arn,
      "${aws_s3_bucket.host_images.arn}/*"
    ]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "host_images" {
  bucket = aws_s3_bucket.host_images.id
  policy = data.aws_iam_policy_document.host_images.json

  # Establish public-access and ACL protections before attaching the policy.
  depends_on = [
    aws_s3_bucket_public_access_block.host_images,
    aws_s3_bucket_ownership_controls.host_images
  ]
}
