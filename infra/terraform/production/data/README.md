# Production data tier

This Terraform root creates only the production Amazon RDS for MySQL data tier.
It does not create application compute, load balancing, application object
storage, identity, deployment automation or application schema objects.

## Architecture

```text
production/foundation remote state
├── private-data-a ─┐
├── private-data-b ─┴─ DB subnet group ── RDS MySQL 8.4
└── database SG ──────────────────────────┘
        ▲
        └── TCP 3306 only from the foundation application SG
```

This root reads the existing network IDs from
`production/foundation/terraform.tfstate`; subnet and security group IDs are
not duplicated in variables. Its own state starts remotely at
`production/data/terraform.tfstate` in the protected production state bucket.

## Database configuration

- MySQL `8.4.10`, pinned to the reviewed 8.4 line;
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
- automatic minor upgrades disabled.

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

`manage_master_user_password = true` instructs RDS to generate the master
password and keep it in an AWS-managed Secrets Manager secret encrypted with
the default Secrets Manager KMS key. Terraform exposes only the secret ARN, not
the password. The application database user and least-privilege grants belong
to a later runtime/bootstrap delivery; the master user must not become the
long-term application credential.

## Flyway startup contract

No migrations run during this delivery. The future production startup order is:

```text
Spring Boot -> TLS connection -> Flyway validate/migrate
            -> Hibernate ddl-auto=validate -> readiness
```

The historical V1-V4 migrations remain immutable. Production keeps
`spring.flyway.baseline-on-migrate=false`.

## Backups and deletion safety

- seven-day automated backup retention;
- backup window `01:00-02:00 UTC`;
- maintenance window `sun:03:00-sun:04:00 UTC`;
- deletion protection enabled;
- final snapshot required;
- automated backups retained when the instance is deleted;
- snapshot tags copied from the instance;
- Terraform `prevent_destroy` enabled.

The final snapshot identifier is stable so normal plans do not change every
day. Before an intentional deletion, choose a new reviewed identifier if a
snapshot with the configured name already exists. A future hibernation workflow
must explicitly and separately disable deletion protection and
`prevent_destroy`; this root does not implement hibernation.

## Validate

Do not run `apply` without an approved plan.

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan -no-color
```
