# Production hibernation record

Production entered hibernation on 2026-09-11. EC2, ALB and RDS compute are
absent. Durable snapshots, S3 objects, the application database secret, ECR,
Cognito, DNS, networking and remote Terraform state remain.

## Retained recovery metadata

| Item | Retained value |
| --- | --- |
| Region | `eu-west-1` |
| Canonical snapshot | `nitros-games-backend-production-hibernation-20260911` |
| Final snapshot | `nitros-games-backend-production-hibernation-final-20260911` |
| Database | `nitrosgames` |
| Observed engine at hibernation | MySQL `8.4.10` |
| Supported engine family | MySQL `8.4` |
| Host-image bucket | `nitros-games-prod-host-images-529601496188-eu-west-1` |
| Last validated SHA | `b6d95eaece3be14c5ea9f5b71fdf4aa41b7719ae` |
| Last validated digest | `sha256:f9823528ca42d686bf6939a593352373f60384dce2e5dea4cac246ef1ffdd606` |

The patch version above is an observation, not a restore contract. A restore
accepts an encrypted, available MySQL snapshot whose engine belongs to the
`mysql8.4` parameter-group family.

## Authoritative desired state

`/nitros-games-backend/production/lifecycle/state` is the sole persistent
operational input:

```text
desiredState=ACTIVE      -> data database_enabled=true; runtime_enabled=true
desiredState=HIBERNATED  -> data database_enabled=false; runtime_enabled=false
```

Those booleans are Terraform locals and cannot be supplied independently.
`STOPPED` is detected when desired state is still `ACTIVE` but existing EC2 and
RDS compute are stopped. Temporary `*_hibernation_authorized` variables permit
only a reviewed deletion plan; they never persist desired state.

Release metadata is intentionally separate at
`/nitros-games-backend/production/lifecycle/release`. See
[`PRODUCTION_LIFECYCLE.md`](PRODUCTION_LIFECYCLE.md) for workflows, failure
recovery, IAM boundaries and the full rehearsal.

## Retained costs

Hibernation removes EC2, ALB, public IPv4 and RDS compute costs. Snapshot and
S3 storage, the application Secrets Manager secret, ECR storage, the Route 53
hosted zone/domain and the Terraform-state bucket can still incur charges.
No retained resource should be deleted merely to make a plan clean.
