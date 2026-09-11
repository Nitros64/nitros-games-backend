# Production runtime

This Terraform root owns the first disposable production application runtime
and its public API endpoint. Production foundation owns the VPC, subnets,
application security group and Route 53 public hosted zone; production data
owns RDS, its managed secret and the host-image S3 bucket. Destroying this root
must not destroy either persistent state or the public hosted zone.

Production is currently hibernated. `runtime_enabled` defaults to `false`, the
runtime state is empty, and a normal plan creates nothing. All 23 managed
runtime resources are gated by that flag, including EC2, ALB, ACM, DNS, IAM and
security-group rules. Re-enablement must be explicit and must happen only after
database restoration. See [`../HIBERNATION.md`](../HIBERNATION.md).

## Enabled architecture

```text
Internet
   |-- HTTP :80 -- 301 redirect --|
   `-- HTTPS :443 -----------------|
                                    v
                         public Application Load Balancer
                         across public-a and public-b
                                    |
                          ALB SG -> TCP/8080
                                    |
                         application SG -> one AL2023
                         x86_64 t3.small EC2
        |-- port 8080 from the ALB SG only
        |-- no SSH ingress
        |-- SSM Session Manager
        |-- ECR pull-only
        |-- Secrets Manager: read-only application DB secret
        `-- S3: exact bucket host-images/*
        |
        `-- application SG -> TCP/3306 -> database SG -> private RDS
```

Route 53 maps only `api.nitrosgames64.com` to the IPv4 ALB. ACM issues a
dedicated DNS-validated certificate for that hostname and the HTTPS listener
uses TLS 1.2/1.3. Port 80 performs only a permanent HTTPS redirect. The ALB
security group can reach port 8080 only on the application security group, and
the application security group accepts port 8080 only from the ALB security
group. Port 22 remains closed and SSM remains the sole administrative path.
There is no NAT Gateway, VPC endpoint, Auto Scaling Group, ECS or Kubernetes
resource. The apex domain and `www` remain unconfigured for the future Angular
frontend.

## Remote-state contracts

The root stores its own state at:

```text
s3://nitros-games-backend-tfstate-529601496188-eu-west-1/production/runtime/terraform.tfstate
```

It reads the VPC, both public subnets, application security group and public
hosted-zone ID from `production/foundation/terraform.tfstate`, and RDS/S3
identifiers from `production/data/terraform.tfstate`. The EC2 runtime remains
in public subnet `a`; the ALB spans public subnets `a` and `b`. Terraform
performs no mutation of either source state object, although this runtime state
owns the narrowly scoped ingress rule attached to the foundation-owned
application security group.

Remote-state availability checks are enforced only when
`runtime_enabled=true`. This lets the hibernated runtime plan safely consume
null RDS outputs without weakening the contracts used during restoration.

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

The separate GitHub production deployer role trusts only the OIDC subject
`repo:Nitros64/nitros-games-backend:environment:production` with audience
`sts.amazonaws.com`. It may verify images in the exact application ECR
repository, send `AWS-RunShellScript` only to the production EC2 instance, and
read SSM command/instance status. It has no Secrets Manager, S3, RDS, SSH, ECR
push or general infrastructure permissions. Application secrets remain the
responsibility of the EC2 runtime role.

## Runtime and deployment contract

`deploy/production/compose.yaml` contains only the API. There is no MySQL,
Keycloak or host-image volume. It preserves a read-only root filesystem,
`no-new-privileges`, dropped capabilities, a bounded `/tmp`, resource limits
and the readiness probe.

`deploy/production/deploy.sh` is the shared manual/CD deployment entry point.
It requires an immutable ECR URI tagged with a full 40-character commit SHA and
a root-owned `/opt/nitros-games/runtime.conf` containing only non-secret values:

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
`runtime.conf` with fake OAuth values.

`.github/workflows/cd-production.yml` is manually dispatched from `main` with
a full `commit_sha`. Before entering the protected `production` Environment it
proves the commit belongs to `main` and has a successful push CI run. After
approval, it assumes the OIDC role, confirms that the corresponding immutable
ECR image already exists, transports only `compose.yaml` and `deploy.sh` from
that exact commit through SSM, and runs the established readiness/rollback
contract. It then checks the public readiness and catalog GET endpoints. It
does not build or publish an image and never transports `runtime.env`.

Configure these non-secret GitHub Environment variables after applying the IAM
plan:

```text
AWS_REGION=eu-west-1
AWS_ECR_REPOSITORY=nitros-games-backend
PRODUCTION_INSTANCE_ID=<ec2_instance_id output>
AWS_PRODUCTION_DEPLOY_ROLE_ARN=<github_actions_production_role_arn output>
```

The `production` GitHub Environment and its required reviewers/allowed branch
policy are repository settings and are intentionally not managed by this
Terraform root.

## Terraform workflow

```shell
cd infra/terraform/production/runtime
terraform fmt -check
terraform init
terraform validate
terraform plan -no-color
```

The normal hibernated plan must report `No changes`. After RDS has been restored
and verified, prepare an explicit reviewed enablement plan with:

```shell
terraform plan -var='runtime_enabled=true' -out=production-runtime-restore.tfplan
```

Do not apply until that plan has been reviewed. No `moved` blocks were needed
for the hibernation flag because the runtime state was already empty before the
23 resource addresses became conditional.

## Incremental cost estimate

At roughly 730 running hours per month in `eu-west-1`, budget approximately:

- `t3.small` Linux On-Demand: about USD 17-18/month;
- 20 GiB gp3 root EBS: about USD 1.75-2/month;
- one public IPv4 at USD 0.005/hour: about USD 3.65/month;
- normal data transfer and log storage, if any, are additional.

While `runtime_enabled=false`, none of these runtime resources exists, so this
runtime-only baseline is zero. Retained snapshots, S3, Secrets Manager, ECR,
Route 53/domain registration and the Terraform backend may still incur their
own storage or recurring charges; see the H5 inventory.
