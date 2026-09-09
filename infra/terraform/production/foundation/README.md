# Production network foundation

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

## Local state for Delivery 8B.1

State remains local for this delivery. Do not create production data resources
until a separate delivery has created a protected remote backend and migrated
this state. Never commit `terraform.tfvars`, plan files or state files.

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
