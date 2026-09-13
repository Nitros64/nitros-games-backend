# Production data tier

This Terraform root owns the persistent production data resources: Amazon RDS
for MySQL and the private S3 bucket for uploaded host images. It does not create
application compute, load balancing, identity, deployment automation or
application schema objects.

Production is currently hibernated. The RDS instance is absent while two
encrypted recovery snapshots, the application database secret metadata, S3,
the subnet group and the parameter group remain. See
[`../HIBERNATION.md`](../HIBERNATION.md) for the authoritative recovery record
and restoration order.

## Architecture

```text
production/foundation remote state
├── private-data-a ─┐
├── private-data-b ─┴─ DB subnet group ── RDS MySQL 8.4
└── database SG ──────────────────────────┘
        ▲
        └── TCP 3306 only from the foundation application SG

Application runtime (future) ── TLS ── S3 host-images bucket

Secrets Manager
├── RDS-managed master credential (bootstrap only)
└── application database secret metadata (runtime credential target)
```

This root reads the existing network IDs from
`production/foundation/terraform.tfstate`; subnet and security group IDs are
not duplicated in variables. Its own state starts remotely at
`production/data/terraform.tfstate` in the protected production state bucket.

## Host-image object storage

The bucket name is derived from the project, production environment, AWS
account and region:

```text
nitros-games-prod-host-images-529601496188-eu-west-1
```

The longer unabridged name would exceed S3's 63-character limit. The bucket is
separate from the Terraform state bucket and has these protections:

- all four S3 Block Public Access settings enabled;
- ACLs disabled through `BucketOwnerEnforced`;
- default SSE-S3 encryption using AES-256;
- versioning enabled;
- bucket policy denying every request where `aws:SecureTransport` is false;
- `force_destroy = false` and Terraform `prevent_destroy`;
- no public allow policy, website hosting, public ACL or lifecycle expiration.

Objects will eventually use keys shaped as:

```text
host-images/<uuid>.<extension>
```

No placeholder object is needed because S3 prefixes are logical. The current
application still uses the filesystem implementation selected as
`FileHostImageStorage`; the S3 adapter and its Spring profile selection belong
to Delivery 8B.5.

The future runtime role should receive only `s3:GetObject`, `s3:PutObject` and
`s3:DeleteObject` on `<bucket-arn>/host-images/*`, plus `s3:ListBucket`
restricted to the `host-images/` prefix. This delivery creates no EC2 role or
runtime IAM policy.

## Database configuration

- MySQL `8.4` family with automatic compatible minor/patch upgrades enabled;
- `db.t4g.micro`, Single-AZ;
- 20 GiB encrypted gp3 storage;
- storage autoscaling up to 100 GiB;
- private endpoint in both production private data subnets;
- no public access;
- initial database `nitrosgames`;
- master username `nitros_admin`;
- password generated and managed by RDS in AWS Secrets Manager;
- standard CloudWatch metrics only, without Enhanced Monitoring or Performance
  Insights;
- major upgrades disabled.

Storage autoscaling increases allocated capacity as required, up to 100 GiB.
It does not automatically shrink storage later, so increases are effectively
one-way.

## Charset and TLS

The custom `mysql8.4` parameter group sets:

```text
character_set_server     = utf8mb4
collation_server         = utf8mb4_unicode_ci
require_secure_transport = ON
```

AWS reports all three parameters as dynamic for `mysql8.4`; they use
`apply_method = immediate` and do not require a reboot.

`require_secure_transport` rejects non-TLS database connections. A future
runtime delivery must construct `DB_URL` with `sslMode=VERIFY_IDENTITY` and
configure the current Amazon RDS CA trust chain. The staging-only
`sslMode=DISABLED` URL must not be reused in production.

## Credentials

When RDS exists, `manage_master_user_password = true` instructs it to generate
the master password and keep it in an RDS-managed Secrets Manager secret.
Terraform exposes only the secret ARN, not the password. AWS removed that
RDS-managed secret with the H4 database deletion. The independently managed
`nitros_app` application secret remains retained for restoration.

## Flyway startup contract

No migrations run during this delivery. The future production startup order is:

```text
Spring Boot -> TLS connection -> Flyway validate/migrate
            -> Hibernate ddl-auto=validate -> readiness
```

The historical V1-V4 migrations remain immutable. Production keeps
`spring.flyway.baseline-on-migrate=false`.

## Secret classification and application database user

Secrets include database passwords, OAuth client secrets and administrative
credentials. DB endpoint, port, database name, AWS region, S3 bucket name,
OAuth issuer URI, JWK Set URI and OAuth audience are ordinary configuration and
must not be placed in Secrets Manager merely because they vary by environment.

The RDS master credential remains exclusively in the RDS-managed Secrets
Manager secret created by `manage_master_user_password = true`. Terraform does
not read, duplicate or output its password.

Terraform creates only metadata for the dedicated application credential at:

```text
nitros-games-backend/production/database/application
```

There is deliberately no `aws_secretsmanager_secret_version` or password in
Terraform state. The eventual `SecretString` is initialized outside Terraform
and contains only:

```json
{"username":"nitros_app","password":"<generated-at-bootstrap>"}
```

The dedicated `nitros_app` account is reconciled by an idempotent, short-lived
bootstrap run inside the production VPC. It receives `ALL PRIVILEGES` only on
`nitrosgames.*`, without `WITH GRANT OPTION`, and uses `REQUIRE SSL`. This lets
Flyway perform database-scoped DDL and DML but grants no global `*.*`, user
administration, server administration or file-system privilege.

The controlled transition has two phases:

1. Apply this data root to create the metadata-only secret and publish
   `application_db_secret_arn` in remote state.
2. Update runtime from that output, temporarily allowing the exact EC2 role to
   read the master secret and read/write the application secret. Run the
   one-time bootstrap through SSM, verify a TLS `SELECT 1`, then remove master
   access and `PutSecretValue`. Normal runtime retains only
   `GetSecretValue`/`DescribeSecret` on the application secret.

Terraform never connects to private MySQL through a MySQL provider,
`local-exec`, `remote-exec` or a provisioner.

## Backups and deletion safety

- seven-day automated backup retention;
- backup window `01:00-02:00 UTC`;
- maintenance window `sun:03:00-sun:04:00 UTC`;
- deletion protection enabled whenever RDS is restored normally;
- final snapshot required;
- automated backups retained when the instance is deleted;
- snapshot tags copied from the instance;
- an explicit hibernation authorization is required before a plan may remove
  an existing RDS instance after desired state becomes `HIBERNATED`.

The canonical manual recovery point is managed as
`aws_db_snapshot.hibernation` with identifier
`nitros-games-backend-production-hibernation-20260911` and Terraform
`prevent_destroy`. Its source is the stable database identifier rather than a
counted instance expression, so the snapshot remains valid and managed while
desired state is `HIBERNATED`. A block-level dependency orders its initial creation
after the live RDS instance without retaining an invalid `[0]` reference.

The SSM lifecycle parameter is the only persistent desired-state source.
Terraform derives `database_enabled` internally: `ACTIVE` creates/retains RDS
and `HIBERNATED` removes it. There is no operator-provided
`database_enabled`. The checked-in `database_hibernation_authorized=false`
default prevents an ordinary plan from deleting an RDS instance after an
unexpected state transition.

Temporary destructive authorization must never be committed in an
automatically loaded `*.auto.tfvars` file. Lifecycle automation passes only
the temporary authorization and unique final-snapshot input explicitly:

```hcl
database_hibernation_authorized = true
restore_snapshot_identifier     = null
hibernation_snapshot_identifier = "nitros-games-backend-production-hibernation-20260911"
final_snapshot_identifier       = "nitros-games-backend-production-hibernation-final-run-<github-run-id>"
```

Normal planning never enables that authorization. `deploy/production/lifecycle.sh`
inspects saved plan JSON and permits only the expected RDS update/create/delete
address for the applicable lifecycle phase.

Before a later intentional deletion, set `final_snapshot_identifier` to a new
value matching
`nitros-games-backend-production-hibernation-final-run-<github-run-id>`.
Validation requires that unique operational form and prevents it from matching
the canonical manual snapshot.

`restore_snapshot_identifier` is transient and used only during creation. The
ordinary ACTIVE configuration later omits it. Terraform ignores that one
ForceNew provenance attribute so normalization cannot replace the restored
database; the restore workflow requires the resulting normal plan to be empty.

S3 is persistent data and should normally remain untouched while production
compute is stopped or destroyed. Its storage and retained object versions will
continue to incur a small charge during compute hibernation.

## Validate

Do not run `apply` without an approved plan.

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan -no-color
```
