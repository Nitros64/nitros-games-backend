# Production lifecycle runbook

## Control plane

Production has two non-secret String parameters owned by the remote-state
`production/foundation` root and protected with `prevent_destroy`:

- `/nitros-games-backend/production/lifecycle/state` stores schema version,
  `ACTIVE` or `HIBERNATED`, last transition run and timestamp.
- `/nitros-games-backend/production/lifecycle/release` stores the last
  successful immutable application SHA, image digest and timestamp.

Terraform ignores operational changes to each value while retaining ownership
of the parameters. Data and runtime read the live state parameter and derive
resource counts internally. No Git commit or tfvars edit changes operational
state.

All five production workflows use the protected `production` GitHub
Environment and concurrency group `production-control`:

| Workflow | Required starting state | Result |
| --- | --- | --- |
| `CD - Production` | `ACTIVE` | Deploys an existing full-SHA ECR image and updates only release metadata. |
| `Stop Production` | `ACTIVE` or already `STOPPED` | Stops EC2, then RDS, or verifies the existing stop; desired state remains `ACTIVE`. A fully `HIBERNATED` site is reported without mutation. |
| `Start Production` | `STOPPED` or already `ACTIVE` | Starts RDS/EC2 when needed and always verifies public readiness. It refuses `HIBERNATED` and directs the operator to Restore. |
| `Hibernate Production` | `STOPPED` | Creates a tagged snapshot, transitions to `HIBERNATED`, and removes runtime/RDS through whitelisted plans. |
| `Restore Production` | `HIBERNATED` | Transitions to `ACTIVE`, restores RDS/runtime, deploys a validated SHA and proves stable plans. |

`deploy/production/lifecycle.sh` provides exact JSON validation, state
transitions, tagged EC2 discovery, snapshot compatibility checks, waiters and
Terraform plan whitelists. State transitions are read-after-write verified.

## Security boundaries

The permanent lifecycle role trusts only:

```text
aud = sts.amazonaws.com
sub = repo:Nitros64/nitros-games-backend:environment:production
```

It may read foundation/data/identity/runtime state, write only data/runtime
state, read both control parameters, update lifecycle state and update release
metadata after a successful restore. Its AWS permissions are limited to the
known production RDS/runtime lifecycle, tagged compute, exact Terraform state
objects, exact ECR repository inspection, snapshot creation/tagging, metadata
inspection of the retained bucket/application secret, and SSM commands on the
tagged application instance. It cannot read a SecretString. Permissions are
four small customer-managed policies (`state`, `data`, `runtime-compute` and
`runtime-edge`), so the role consumes no aggregate inline-policy quota.

The disposable deployment role has no Terraform or lifecycle-state write. It
can discover the tagged EC2, inspect the exact ECR repository, deploy through
SSM, read both parameters and update only the release parameter.

## Hibernate safety

Hibernate requires the literal `HIBERNATE_PRODUCTION` and a fully stopped
site. Snapshot and final-snapshot names contain the GitHub run ID. The manual
snapshot receives non-secret tags for project, environment, lifecycle run,
timestamp, deployed SHA/digest and current application-secret version ID.

The workflow then accepts only these plan shapes:

1. zero or one RDS `update` limited exactly to `deletion_protection` and
   `final_snapshot_identifier`;
2. deletion of only the known 23 runtime addresses;
3. deletion of only `aws_db_instance.mysql[0]` with final snapshot enabled;
4. ordinary hibernated data/runtime plans with zero changes.

S3, the application secret, the canonical protected snapshot, ECR, Cognito,
foundation and the registered domain never enter a destroy plan.

## Restore safety

Restore requires a full main SHA with successful CI, its existing immutable
ECR image and an available encrypted snapshot compatible with MySQL 8.4. It
permits only one RDS create and the known runtime create addresses. EC2 is
discovered by `Name`, `Project`, `Environment` and `Component` tags; a missing
or duplicate instance stops the operation.

`restore_snapshot_identifier` is a create-only provenance input. The RDS
resource ignores later drift for that single ForceNew attribute. After restore,
the workflow deliberately omits it and requires a normal ACTIVE data plan to
be empty:

```text
RDS_REPLACEMENT=no
RDS_CHANGE=no
PLAN_DESTROY=0
```

The restored DB check connects as `nitros_app` using RDS CA validation, rejects
failed Flyway history, records the current history before startup, deploys the
selected application so pending migrations run, and verifies non-regressing
history after readiness. It reports the observed Flyway version dynamically;
no V4/V5 value is built into the lifecycle.

## Failure and retry behavior

State transitions occur only after preconditions and snapshot/image checks.
`lastTransitionRun` stores an operation marker, not only a transient workflow
ID. Restore records its original run, snapshot and immutable SHA; Hibernate
records its original run, from which both snapshot names are derived. A later
workflow invocation validates and reuses those exact inputs, then converges the
remaining RDS/runtime subset without a manual SSM reset. Changed restore inputs,
malformed markers, replacement actions, unrelated addresses or unrelated RDS
updates fail closed.

## One-time setup after review

After the foundation plan is separately approved and applied, configure the
protected GitHub `production` Environment variable:

```text
AWS_PRODUCTION_LIFECYCLE_ROLE_ARN=<foundation github_actions_production_lifecycle_role_arn>
```

Retain the existing `AWS_REGION`, `AWS_ECR_REPOSITORY` and, while runtime is
active, `AWS_PRODUCTION_DEPLOY_ROLE_ARN`. Remove obsolete
`PRODUCTION_INSTANCE_ID`; discovery is dynamic.

No lifecycle workflow should be run until foundation has created both SSM
parameters and the permanent role, and ordinary hibernated data/runtime plans
have been reviewed as no-op.

## First end-to-end rehearsal

1. Apply only the approved foundation control-plane plan.
2. Configure `AWS_PRODUCTION_LIFECYCLE_ROLE_ARN` and required reviewers.
3. Confirm lifecycle state is `HIBERNATED`; plan data/runtime normally and
   require no changes.
4. Dispatch Restore with a retained snapshot and a full main SHA that passed
   CI and exists in ECR.
5. Verify RDS family/TLS, dynamic Flyway checks, Hibernate validation,
   container readiness, public readiness/API GET, and normalized no-op plans.
6. Dispatch Stop; verify state is detected as `STOPPED` while SSM remains
   `ACTIVE`.
7. Dispatch Start; verify the same resources and image return to `ACTIVE`.
8. Dispatch Stop again.
9. Dispatch Hibernate with the exact confirmation; verify recovery/final
   snapshots and zero-change hibernated plans.
10. Inspect that S3, application secret, ECR, Cognito, foundation, hosted zone
    and domain remain present. Do not run a second cycle until costs and
    retained snapshots are reviewed.
