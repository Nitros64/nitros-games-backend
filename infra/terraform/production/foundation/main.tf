data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
  availability_zones = {
    a = data.aws_availability_zones.available.names[0]
    b = data.aws_availability_zones.available.names[1]
  }
}

check "two_distinct_availability_zones" {
  assert {
    condition     = length(distinct(values(local.availability_zones))) == 2
    error_message = "Production requires two distinct availability zones."
  }
}

resource "aws_vpc" "production" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${local.name_prefix}-vpc"
  }
}

resource "aws_internet_gateway" "production" {
  vpc_id = aws_vpc.production.id

  tags = {
    Name = "${local.name_prefix}-igw"
  }
}

resource "aws_subnet" "public" {
  for_each = var.public_subnet_cidrs

  vpc_id                  = aws_vpc.production.id
  cidr_block              = each.value
  availability_zone       = local.availability_zones[each.key]
  map_public_ip_on_launch = true

  tags = {
    Name = "${local.name_prefix}-public-${each.key}"
    Tier = "public"
  }
}

resource "aws_subnet" "private_data" {
  for_each = var.private_data_subnet_cidrs

  vpc_id                  = aws_vpc.production.id
  cidr_block              = each.value
  availability_zone       = local.availability_zones[each.key]
  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name_prefix}-private-data-${each.key}"
    Tier = "private-data"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.production.id

  tags = {
    Name = "${local.name_prefix}-public"
    Tier = "public"
  }
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.production.id
}

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

# A route table per AZ keeps the data tier ready for future independent routing.
# They intentionally contain no default route, Internet Gateway, NAT Gateway or
# NAT instance route; only the implicit local VPC route will exist.
resource "aws_route_table" "private_data" {
  for_each = var.private_data_subnet_cidrs

  vpc_id = aws_vpc.production.id

  tags = {
    Name = "${local.name_prefix}-private-data-${each.key}"
    Tier = "private-data"
  }
}

resource "aws_route_table_association" "private_data" {
  for_each = aws_subnet.private_data

  subnet_id      = each.value.id
  route_table_id = aws_route_table.private_data[each.key].id
}

resource "aws_security_group" "application" {
  name        = "${local.name_prefix}-application"
  description = "Production application compute; no inbound access in the foundation phase."
  vpc_id      = aws_vpc.production.id

  tags = {
    Name = "${local.name_prefix}-application"
  }
}

resource "aws_vpc_security_group_egress_rule" "application_ipv4" {
  security_group_id = aws_security_group.application.id
  description       = "Allow application runtime outbound access; ingress remains empty."
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_security_group" "database" {
  name        = "${local.name_prefix}-database"
  description = "Production database; MySQL only from the application security group."
  vpc_id      = aws_vpc.production.id

  tags = {
    Name = "${local.name_prefix}-database"
  }
}

resource "aws_vpc_security_group_ingress_rule" "database_mysql_from_application" {
  security_group_id            = aws_security_group.database.id
  referenced_security_group_id = aws_security_group.application.id
  description                  = "Allow MySQL only from production application compute."
  from_port                    = 3306
  to_port                      = 3306
  ip_protocol                  = "tcp"
}
