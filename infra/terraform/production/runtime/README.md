# Production runtime

This Terraform root owns only the first disposable production application
runtime. Production foundation owns the VPC, subnets and security groups;
production data owns RDS, its managed secret and the host-image S3 bucket.
Destroying this root must not destroy either persistent state.

## Architecture

```text
Internet (outbound only)
        |
Production IGW
        |
public-a + ephemeral public IPv4
        |
one AL2023 x86_64 t3.small EC2
        |-- existing application SG (zero ingress)
        |-- SSM Session Manager
        |-- ECR pull-only
        |-- Secrets Manager: read-only application DB secret
        `-- S3: exact bucket host-images/*
        |
        `-- application SG -> TCP/3306 -> database SG -> private RDS
```

There is no SSH key, inbound security-group rule, ALB, NAT Gateway, VPC
endpoint, Auto Scaling Group, ECS or Kubernetes resource. The public IPv4 is
used only for outbound access through the existing Internet Gateway. Until an
ALB exists, the future Compose runtime binds the API only to
`127.0.0.1:8080`.

## Remote-state contracts

The root stores its own state at:

```text
s3://nitros-games-backend-tfstate-529601496188-eu-west-1/production/runtime/terraform.tfstate
```

It reads network identifiers from
`production/foundation/terraform.tfstate` and RDS/S3 identifiers from
`production/data/terraform.tfstate`. The runtime selects public subnet `a` and
attaches exactly the exported application security group. Terraform performs
no mutation of either source state.

Bootstrap owns the ECR repository in local bootstrap state, so this root looks
up the existing `nitros-games-backend` repository by name rather than copying
an ARN.

## Host bootstrap

User data only prepares the machine. It enables the preinstalled AL2023 SSM
Agent, installs and starts Docker, installs `jq`, downloads the pinned Docker
Compose x86_64 binary and verifies its SHA-256, and creates:

```text
/opt/nitros-games       root:root 0750
/run/nitros-games       root:root 0700
```

It does not pull an application image, retrieve secrets, run Flyway, create an
environment file or start Spring Boot. The AMI comes from the AWS public AL2023
SSM parameter. The EC2 resource ignores later AMI-value drift so replacement
requires an explicit reviewed `-replace=aws_instance.application` operation.

IMDSv2 is mandatory. Its response hop limit is `2`, which is required for the
containerized AWS SDK to obtain the instance-role credentials through the
Docker network namespace.

## IAM boundaries

The EC2 role has the AWS-managed `AmazonSSMManagedInstanceCore` policy plus
three inline policies:

- ECR: `ecr:GetAuthorizationToken` on `*` because the ECR authorization API
  does not support repository scoping; layer/image reads are limited to the
  existing backend repository ARN.
- S3: `GetObject`, `PutObject` and `DeleteObject` only on the data-state bucket
  ARN plus `/host-images/*`; `ListBucket` is restricted with `s3:prefix` to
  `host-images/` and `host-images/*`.
- Secrets Manager: `DescribeSecret` and `GetSecretValue` only for the exact
  application database secret exported by production data.

The runtime role has no access to the RDS master credential, cannot write the
application secret and cannot call `GetRandomPassword`. The retained
`bootstrap-db-user.sh` is an operational recovery tool and cannot run under this
final role. Reusing it requires a separately reviewed, temporary IAM elevation
that is removed immediately after reconciliation and verification. Terraform
performs no SQL provisioning and uses no Terraform provisioner or MySQL
provider.

## Runtime and deployment contract

`deploy/production/compose.yaml` contains only the API. There is no MySQL,
Keycloak or host-image volume. It preserves a read-only root filesystem,
`no-new-privileges`, dropped capabilities, a bounded `/tmp`, resource limits
and the readiness probe.

`deploy/production/deploy.sh` is a manual future deployment entry point. It
requires an immutable ECR URI tagged with a full 40-character commit SHA and a
root-owned `/opt/nitros-games/runtime.conf` containing only non-secret values:

```text
APPLICATION_DB_SECRET_ARN=<data-state application_db_secret_arn>
DB_URL=jdbc:mysql://<actual-rds-endpoint>:3306/nitrosgames?sslMode=VERIFY_IDENTITY
APP_STORAGE_HOST_IMAGES_S3_BUCKET=nitros-games-prod-host-images-529601496188-eu-west-1
APP_SECURITY_ALLOWED_ORIGINS=<real production origins>
OAUTH2_ISSUER_URI=<real production issuer>
OAUTH2_JWK_SET_URI=<real production JWK endpoint>
OAUTH2_RESOURCE_ID=<identity-state API resource ID>
OAUTH2_ACCESS_SCOPE=<identity-state API access scope>
OAUTH2_ADMIN_SCOPE=<identity-state API admin scope>
OAUTH2_ALLOWED_CLIENT_IDS=<comma-separated trusted client IDs>
```

The script validates all configuration before retrieving the exact application
database secret, requires its username to be `nitros_app`,
authenticates to ECR, writes the expanded environment atomically to
`/run/nitros-games/runtime.env` with mode `0600`, pulls before changing the
running image, waits for readiness and restores the previous immutable image
when possible. It never prints the database password or ECR token. Rollback
cannot reverse Flyway migrations, so migrations must remain backward
compatible.

Use only reviewed outputs from the production identity root. Do not create
`runtime.conf` with fake OAuth values. The application remains undeployed until
the identity plan is applied and an initial administrator is provisioned.

## Terraform workflow

```shell
cd infra/terraform/production/runtime
terraform fmt -check
terraform init
terraform validate
terraform plan -no-color
```

Do not apply until the plan has been reviewed. Expected changes are seven
runtime resources: one IAM role, one SSM policy attachment, three inline IAM
policies, one instance profile and one EC2 instance. Foundation, RDS and S3
must show no changes or destroys.

## Incremental cost estimate

At roughly 730 running hours per month in `eu-west-1`, budget approximately:

- `t3.small` Linux On-Demand: about USD 17-18/month;
- 20 GiB gp3 root EBS: about USD 1.75-2/month;
- one public IPv4 at USD 0.005/hour: about USD 3.65/month;
- normal data transfer and log storage, if any, are additional.

The expected runtime-only baseline is therefore roughly USD 23/month, excluding
the existing RDS/S3 and future ALB/NAT. Stopping EC2 stops instance compute and
releases its automatically assigned public IPv4; the EBS volume remains
billable. RDS continues billing independently.
