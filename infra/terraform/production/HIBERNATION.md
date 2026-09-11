# Production hibernation record

Production intentionally entered its stable hibernated state on 2026-09-11.
The application runtime and RDS compute are absent. Durable recovery, storage,
identity, DNS, networking and Terraform-state resources remain managed.

## Recovery metadata

| Item | Retained value |
| --- | --- |
| AWS region | `eu-west-1` |
| Canonical RDS snapshot | `nitros-games-backend-production-hibernation-20260911` |
| Final RDS snapshot | `nitros-games-backend-production-hibernation-final-20260911` |
| Database | `nitrosgames` |
| Database engine | MySQL `8.4.10` |
| Previous RDS class | `db.t4g.micro` |
| Host-image S3 bucket | `nitros-games-prod-host-images-529601496188-eu-west-1` |
| ECR repository | `nitros-games-backend` |
| Cognito user pool | `eu-west-1_Nlg5iBfie` |
| Route 53 hosted zone | `Z0180527397MHMI2FO0ZV` |
| Domain | `nitrosgames64.com` |
| Last validated production SHA | `b6d95eaece3be14c5ea9f5b71fdf4aa41b7719ae` |
| ECR digest | `sha256:f9823528ca42d686bf6939a593352373f60384dce2e5dea4cac246ef1ffdd606` |

Both retained RDS snapshots were verified `available`, encrypted, MySQL
8.4.10, and 20 GiB at H5 preparation. No password, token or `SecretString`
belongs in this document or in Terraform variables.

## Stable hibernated configuration

The data root defaults to:

```hcl
database_enabled                = false
database_hibernation_authorized = false
restore_snapshot_identifier     = null
```

The RDS resource retains Terraform `prevent_destroy=true` for future restored
instances. The canonical manual snapshot, application database secret and
host-image bucket retain their own `prevent_destroy` safeguards. The old
`hibernation-operation.tfvars` remains local and ignored; its temporary
authorization must never be committed or auto-loaded.

The runtime root defaults to:

```hcl
runtime_enabled = false
```

Every managed runtime resource uses this gate. A normal plan in either root
must report `No changes` while production is hibernated.

## Controlled restoration order

1. Choose one retained RDS snapshot and verify that it is `available`,
   encrypted and uses the expected MySQL version.
2. Set `database_enabled=true` and set `restore_snapshot_identifier` to that
   exact snapshot in an explicit, reviewed operation file or `-var` arguments.
3. Generate and review the data plan, then restore RDS and wait until it is
   `available`.
4. Verify `nitros_app` connectivity over TLS with
   `sslMode=VERIFY_IDENTITY`; never print the application secret.
5. Verify that the restored database contains Flyway schema version 4.
6. Set `runtime_enabled=true` explicitly and generate a reviewed runtime plan.
7. Apply the approved runtime plan.
8. Update the GitHub `production` Environment variable
   `PRODUCTION_INSTANCE_ID` if the EC2 identifier changed. Also verify the
   production deploy-role ARN output before enabling CD.
9. Deploy the existing immutable ECR image identified by the validated
   40-character SHA; do not rebuild it for restoration.
10. Verify container health, public readiness and a safe public API GET.

Database restoration must precede runtime enablement because the enabled
runtime contract requires non-null RDS outputs. Each apply remains a separate,
explicitly approved operation.

## Retained-resource and cost inventory

| Category | Retained resources | Charge behavior while hibernated |
| --- | --- | --- |
| Foundation/networking | VPC, Internet Gateway, four subnets, route tables, application/database security groups and rules | These constructs normally have no hourly charge by themselves. There is no NAT Gateway, ALB, EC2 or allocated runtime public IPv4. |
| RDS snapshots | Canonical and final encrypted manual snapshots, each representing 20 GiB | Snapshot storage can continue to incur monthly storage charges. |
| Data support | DB subnet group and MySQL 8.4 parameter group | No standalone recurring charge. |
| S3 application data | Private, encrypted, versioned host-image bucket | Stored bytes, retained object versions and requests can incur charges. |
| Secrets Manager | Dedicated `nitros_app` application secret | The retained secret can incur a recurring per-secret charge. The RDS-managed master secret disappeared with RDS. |
| ECR | `nitros-games-backend` and its immutable images | Retained image-layer storage can incur charges. |
| Cognito | User pool, Angular client, resource server, ADMIN group and Cognito domain | With no active traffic it may remain within service allowances, but usage-dependent charges are still possible. |
| Route 53/domain | Public hosted zone and registered `nitrosgames64.com` domain | Hosted-zone recurring charges and annual domain renewal remain. DNS-query charges depend on traffic. |
| Terraform backend | Private, versioned S3 state bucket using native lock files | Small S3 storage, versioning and request charges remain. |

No resource in this inventory should be deleted merely to make a hibernated
plan clean. H5 changes configuration only and performs no Terraform apply.
