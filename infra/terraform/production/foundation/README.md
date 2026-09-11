# Production network foundation

The foundation state also owns the single public Route 53 hosted zone for
`nitrosgames64.com`. Route 53 Domains created this zone during domain
registration; Terraform adopted the existing zone rather than creating a
second one. The zone is protected with `prevent_destroy`, retains its registrar
comment and intentionally remains untagged to match the existing AWS object.
Domain registration itself is not managed by Terraform.

This Terraform root creates only the long-lived NitrosGames production network:

- one VPC that does not overlap staging;
- two public subnets in distinct Availability Zones;
- two private data subnets in the same two Availability Zones;
- an Internet Gateway and public default route;
- isolated private route tables with no internet or NAT route;
- an application security group with no ingress;
- a database security group that accepts MySQL only from the application group.

It does not create RDS, a DB subnet group, S3, Secrets Manager, EC2, ALB, NAT,
VPC endpoints, IAM deployment roles or application resources.

## Network

```text
VPC 10.43.0.0/16
├── AZ a
│   ├── public-a        10.43.0.0/24  -> Internet Gateway
│   └── private-data-a 10.43.10.0/24 -> local route only
└── AZ b
    ├── public-b        10.43.1.0/24  -> Internet Gateway
    └── private-data-b 10.43.11.0/24 -> local route only
```

The private data subnets do not assign public IP addresses. Their route tables
contain no `0.0.0.0/0` route, Internet Gateway, NAT Gateway or NAT instance.

## Remote state migration for Delivery 8B.2

Bootstrap owns a dedicated, protected S3 backend bucket. This root reserves the
state key `production/foundation/terraform.tfstate` and enables native S3 state
locking with `use_lockfile = true`. The bucket and region intentionally remain
partial backend settings because backend blocks cannot use Terraform variables.

Do not initialize this backend until the bootstrap bucket exists and the local
state has been backed up. The approved migration command will provide the
bucket and region through `-backend-config` and use `-migrate-state` to preserve
the existing foundation state. Never commit `terraform.tfvars`, plan files,
state files or state backups.

## Validate and review

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan
```

Review the complete plan before any apply. A new foundation plan must contain no
destroy or replacement operations.
