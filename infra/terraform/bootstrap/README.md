# AWS delivery foundation

This Terraform root creates only the shared resources needed for the first
delivery increment:

- one private ECR repository for the application image;
- one GitHub Actions OIDC provider, unless an existing provider ARN is supplied;
- one least-privilege IAM role that can publish to that repository;
- an ECR lifecycle policy that bounds retained image storage.

It does not create ECS, RDS, networking, load balancers or production resources.

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
provider and the publisher IAM role.

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

## State

This first version deliberately leaves the backend unspecified. Before more
people or automated workflows manage the infrastructure, move the state to a
remote encrypted backend with locking. Never commit local state because it may
contain sensitive infrastructure data.
