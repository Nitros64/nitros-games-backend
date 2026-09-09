# AWS delivery foundation

This Terraform root creates the shared delivery resources that must exist
independently of the production Terraform roots:

- one private ECR repository for the application image;
- one GitHub Actions OIDC provider, unless an existing provider ARN is supplied;
- one least-privilege IAM role that can publish to that repository;
- an ECR lifecycle policy that bounds retained image storage;
- one protected S3 bucket for production Terraform state.

It does not create ECS, RDS, networking, load balancers or application storage.

## Terraform state bucket

The state bucket has a deterministic, globally unique name:

```text
<project-name>-tfstate-<aws-account-id>-<aws-region>
```

With the current defaults this becomes
`nitros-games-backend-tfstate-<aws-account-id>-eu-west-1`. It is separate from
the future application image bucket and has no website configuration or public
bucket policy.

Its protections are:

- S3 Block Public Access fully enabled;
- Object Ownership set to `BucketOwnerEnforced`;
- S3-managed AES-256 server-side encryption;
- versioning enabled;
- `force_destroy = false`;
- Terraform `prevent_destroy` lifecycle protection.

The production state key layout is:

```text
production/foundation/terraform.tfstate
production/data/terraform.tfstate       # reserved for a future delivery
production/runtime/terraform.tfstate    # reserved for a future delivery
```

Terraform 1.10 or newer supports native S3 lockfiles through
`use_lockfile = true`; DynamoDB locking is therefore neither required nor
created.

Minimum IAM access for the foundation state consists of:

- `s3:ListBucket` on the bucket, restricted to the foundation key prefix;
- `s3:GetBucketVersioning` on the bucket when versioning is inspected;
- `s3:GetObject` and `s3:PutObject` on the state object;
- `s3:GetObject`, `s3:PutObject` and `s3:DeleteObject` on its `.tflock` object.

Terraform does not require `s3:DeleteObject` on the state object itself. This
delivery does not grant GitHub access to production state.

S3 cost should be negligible at this scale. Versioning can accumulate old
state revisions, but recovery history is intentionally preserved instead of
using aggressive noncurrent-version expiration.

## Trust boundary

The publisher role can be assumed only by the configured repository and branch.
With the example values, the exact GitHub OIDC subject is:

```text
repo:Nitros64/nitros-games-backend:ref:refs/heads/main
```

Pull request workflows cannot assume this role. The role can authenticate to
ECR and push or read images only in the repository created by this Terraform
root; it has no ECS, RDS or administrator permissions.

## Prepare

Terraform uses the normal AWS credential chain for this one-time bootstrap. The
operator applying it therefore needs permission to manage ECR, the GitHub OIDC
provider, the publisher IAM role and the protected S3 state bucket.

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan -out bootstrap.tfplan
```

Review the plan before running:

```powershell
terraform apply bootstrap.tfplan
```

The apply command creates AWS resources and should not be run from an unreviewed
branch. Local `terraform.tfvars`, plan and state files are ignored by Git.

## Existing GitHub OIDC provider

An AWS account can already contain the shared provider for
`token.actions.githubusercontent.com`. In that case, set
`existing_github_oidc_provider_arn` in `terraform.tfvars`; Terraform will reuse
it instead of trying to create a duplicate.

## Outputs used by the next increment

After apply, these outputs connect GitHub Actions to ECR:

```powershell
terraform output -raw ecr_repository_name
terraform output -raw ecr_repository_url
terraform output -raw github_actions_ecr_role_arn
```

The next increment will add those non-secret values as GitHub repository
variables and extend the existing CI workflow to publish exactly one immutable
image tagged with the merge commit SHA.

## Bootstrap state

Bootstrap deliberately retains local state because it owns the backend bucket.
Putting bootstrap in the bucket it creates would introduce a circular
dependency during initial provisioning and disaster recovery. Treat bootstrap
state as special: keep a secure backup, never commit it, and never casually
destroy this root. Production foundation, data and runtime roots use the remote
bucket instead.
