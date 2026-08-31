# Disposable EC2 staging

This Terraform root creates a private-by-default, single-host staging target:

- a dedicated VPC, public subnet, route table and internet gateway;
- one `t3.small` Amazon Linux 2023 instance with an encrypted 16 GiB root disk;
- a security group with no inbound rules;
- an EC2 role for Systems Manager and read-only access to the application ECR repository;
- a GitHub OIDC role restricted to `main` and to SSM deployment commands for this instance.

The public IPv4 address is used only for outbound access. SSH, the application
port and MySQL are not reachable from the internet. Deployment and smoke tests
run through AWS Systems Manager.

## Disposable data

MySQL and uploaded files live in Docker volumes on the instance root disk. The
disk is encrypted and has `delete_on_termination = true`. Running `terraform
destroy` therefore removes the database and files permanently, which is the
intended lifecycle for this non-public CI/CD demonstration environment.

## Apply

Use the same AWS account and region as the ECR bootstrap:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan -out=staging.tfplan
terraform apply staging.tfplan
```

The apply creates billable EC2, EBS and public IPv4 resources. Destroy the root
when staging is not needed:

```powershell
terraform plan -destroy -out=destroy.tfplan
terraform apply destroy.tfplan
```

Never delete `terraform.tfstate` while the resources exist. This root currently
uses local state and should move to an encrypted remote backend before multiple
operators manage the environment.

## Outputs for GitHub

```powershell
terraform output -raw instance_id
terraform output -raw github_actions_staging_role_arn
terraform output -raw aws_region
```

These values become `STAGING_INSTANCE_ID`, `AWS_STAGING_DEPLOY_ROLE_ARN` and
`AWS_REGION` repository variables in the deployment increment.

## Manual deployment

CI never publishes to AWS. Start the stopped instance first, wait until it is
`Online` in Systems Manager, and then dispatch `CD - Staging` from `main` with
the full SHA of a commit that already has a successful `main` CI run:

```powershell
gh workflow run cd-staging.yml --ref main -f commit_sha=<40-character-main-sha>
```

The workflow verifies the commit and staging availability before building or
publishing. It reuses an existing immutable ECR image for the SHA when possible,
deploys through SSM, and requires the remote readiness smoke test to pass.

Stopping EC2 prevents deployments but does not affect CI. It preserves the EBS
root disk and its Docker volumes; `terraform destroy` removes that temporary
data permanently.
