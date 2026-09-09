# Production identity

This Terraform root owns the production human-identity boundary for
NitrosGames. It deliberately does not depend on the disposable EC2 runtime and
stores state separately at:

```text
s3://nitros-games-backend-tfstate-529601496188-eu-west-1/production/identity/terraform.tfstate
```

No application, ALB, ACM certificate, Route53 record, user, password or client
secret is created by this root.

## Why Cognito

The application is already an OAuth2 resource server and does not use a
Keycloak Java adapter. Its only provider-specific dependency was reading roles
from `realm_access.roles`. Cognito access tokens provide the same human-role
concept through `cognito:groups`, a stable issuer and public JWK Set, and API
authorization through resource-server scopes.

| Concern | Cognito user pool | Production Keycloak |
| --- | --- | --- |
| Existing JWT contract | Small claim/validation adapter | Native `realm_access.roles` |
| Human roles | `cognito:groups` | Realm roles |
| Angular | Public client, Authorization Code + PKCE | Existing public client model |
| M2M | Confidential client + custom scope | Service account + realm role |
| Persistence | AWS-managed | External production database required |
| Operations | No servers, backups or upgrades | Keycloak runtime, DB, backups, patches and upgrades |
| Baseline cost | Usage-based; small human usage is normally inside free tier | Additional always-on compute and database capacity |

Keycloak remains the correct local and staging provider. Running it
professionally in production would require a hardened non-`start-dev` service,
a separate least-privilege database/user, backups, upgrade rehearsals and HTTPS.
That is disproportionate for this repository and would compete with the
application on the current single `t3.small` if colocated.

## Planned architecture

```text
future Angular SPA
  |  Authorization Code + PKCE (no client secret)
  v
AWS-managed Cognito HTTPS domain
  |
  +-- production user pool (self-registration disabled)
  +-- ADMIN group -> cognito:groups in human access tokens
  +-- API resource server
      +-- https://api.nitrosgames64.com/access
      `-- https://api.nitrosgames64.com/admin
  |
  v
Spring Boot resource server
  +-- verifies RS256 signature against the configured JWK Set
  +-- verifies issuer and token lifetime
  +-- rejects Cognito ID tokens (token_use=id)
  +-- verifies client_id/azp against an explicit allowlist
  +-- requires API audience OR an exact API scope
  `-- maps ADMIN group/admin scope to ROLE_ADMIN
```

The resource identifier is an OAuth identifier and does not create DNS. The
default callback and logout URLs assume the documented `nitrosgames64.com`
frontend. Review them before apply; changing them later is in-place. ALB, ACM,
Route53 and the final API HTTPS endpoint remain Delivery 8B.7.

The user pool uses the Lite tier, closed registration, verified email recovery,
optional TOTP MFA, a 14-character password policy and deletion protection. The
Angular client is public, has no secret, permits only the authorization-code
flow and receives the `access` scope. PKCE S256 is supplied by the Angular OIDC
library in each authorization request; no confidential secret belongs in a SPA.

## Token contract

Production Spring configuration is populated from Terraform outputs:

```text
OAUTH2_ISSUER_URI=<issuer_uri>
OAUTH2_JWK_SET_URI=<jwk_set_uri>
OAUTH2_RESOURCE_ID=<api_resource_id>
OAUTH2_ACCESS_SCOPE=<api_access_scope>
OAUTH2_ADMIN_SCOPE=<api_admin_scope>
OAUTH2_ALLOWED_CLIENT_IDS=<angular_client_id>[,<future-m2m-client-id>]
```

Only access tokens are sent to the API. A normal authenticated user receives
the access scope and no application role. Membership in the `ADMIN` Cognito
group adds `cognito:groups=["ADMIN"]`, which maps to `ROLE_ADMIN`. API reads stay
public; mutations and Prometheus continue to require `ROLE_ADMIN`.

The issuer has the form
`https://cognito-idp.eu-west-1.amazonaws.com/<user-pool-id>` and its signing keys
are at `<issuer>/.well-known/jwks.json`. They are public trust material, not a
secret.

## Machine-to-machine boundary

The backend supports M2M tokens whose exact `admin` custom scope maps to
`ROLE_ADMIN`. A production M2M client is intentionally not in this Terraform
plan because `aws_cognito_user_pool_client(generate_secret=true)` writes the
generated client secret into Terraform state. The AWS provider also does not
yet model Cognito's add/delete client-secret rotation operations.

Create a confidential automation client only when a concrete production
consumer exists. It must:

- support only `client_credentials` (or AWS `GetClientToken`);
- be allowed only the `admin` scope needed by that automation;
- keep its secret in a dedicated Secrets Manager or GitHub Environment secret;
- add its generated client ID to `OAUTH2_ALLOWED_CLIENT_IDS`;
- cache tokens until near expiry to reduce requests and cost;
- rotate with Cognito's two concurrent client-secret mechanism.

This preserves a legitimate M2M path without creating an unused privileged
credential or leaking one into Git/Terraform state. Local and staging E2E keep
their development-only Keycloak clients unchanged.

## Future Angular configuration

The Angular repository will eventually need:

```text
authority / issuer = issuer_uri output
client ID          = angular_client_id output
authorization URL  = authorization_endpoint output
token URL          = token_endpoint output
redirect URI       = one exact angular_callback_urls value
scopes             = openid email profile <api_access_scope>
response type      = code
PKCE               = S256
```

It must use the access token, never the ID token, as the API Bearer token. It
must not contain a client secret. Assigning a human administrator remains an
explicit operational action after apply; Terraform creates no users or
passwords.

## Terraform workflow

```shell
cd infra/terraform/production/identity
terraform fmt -check
terraform init
terraform validate
terraform plan -no-color
```

Review the final frontend URLs and the complete plan before apply. This
delivery must stop before `terraform apply`.

## Cost

Cognito Lite and Essentials currently include 10,000 direct/social MAUs per
month in the ongoing free tier, so this project's very small human usage should
normally add USD 0/month. Email/SMS delivery and federation can add usage
charges. M2M has no free tier and is charged per successful token response, so
an automation client should request and cache tokens only when needed.

A production Keycloak alternative would add at least one continuously operated
runtime plus managed database capacity/backups (or enlarge and couple the
current runtime/RDS), making its practical baseline tens of dollars per month
before engineering time.
