terraform {
  backend "s3" {
    bucket       = "nitros-games-backend-tfstate-529601496188-eu-west-1"
    key          = "production/runtime/terraform.tfstate"
    region       = "eu-west-1"
    encrypt      = true
    use_lockfile = true
  }
}
