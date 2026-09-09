terraform {
  backend "s3" {
    key          = "production/foundation/terraform.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}
