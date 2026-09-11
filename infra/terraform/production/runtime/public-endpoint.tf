locals {
  public_endpoint_name = "${var.project_name}-prod-api"
}

resource "aws_security_group" "load_balancer" {
  name        = "${local.name_prefix}-load-balancer"
  description = "Public HTTPS entry point for the production API."
  vpc_id      = data.terraform_remote_state.foundation.outputs.vpc_id

  tags = {
    Name = "${local.name_prefix}-load-balancer"
  }
}

resource "aws_vpc_security_group_ingress_rule" "load_balancer_https_ipv4" {
  security_group_id = aws_security_group.load_balancer.id
  description       = "Accept public HTTPS traffic."
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "load_balancer_http_redirect_ipv4" {
  security_group_id = aws_security_group.load_balancer.id
  description       = "Accept HTTP only for permanent redirection to HTTPS."
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "load_balancer_to_application" {
  security_group_id            = aws_security_group.load_balancer.id
  referenced_security_group_id = data.terraform_remote_state.foundation.outputs.application_security_group_id
  description                  = "Forward API traffic only to the application security group."
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "application_from_load_balancer" {
  security_group_id            = data.terraform_remote_state.foundation.outputs.application_security_group_id
  referenced_security_group_id = aws_security_group.load_balancer.id
  description                  = "Accept API traffic only from the production load balancer."
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}

resource "aws_lb" "application" {
  name                       = local.public_endpoint_name
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.load_balancer.id]
  subnets                    = [for key in sort(keys(data.terraform_remote_state.foundation.outputs.public_subnet_ids)) : data.terraform_remote_state.foundation.outputs.public_subnet_ids[key]]
  ip_address_type            = "ipv4"
  enable_deletion_protection = false
  drop_invalid_header_fields = true

  tags = {
    Name = "${local.name_prefix}-api"
  }
}

resource "aws_lb_target_group" "application" {
  name                 = local.public_endpoint_name
  port                 = 8080
  protocol             = "HTTP"
  target_type          = "instance"
  vpc_id               = data.terraform_remote_state.foundation.outputs.vpc_id
  deregistration_delay = 30

  health_check {
    enabled             = true
    path                = "/actuator/health/readiness"
    protocol            = "HTTP"
    port                = "traffic-port"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name = "${local.name_prefix}-api"
  }
}

resource "aws_lb_target_group_attachment" "application" {
  target_group_arn = aws_lb_target_group.application.arn
  target_id        = aws_instance.application.id
  port             = 8080
}

resource "aws_acm_certificate" "api" {
  domain_name       = var.api_domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "api_certificate_validation" {
  for_each = {
    for option in aws_acm_certificate.api.domain_validation_options :
    option.domain_name => {
      name   = option.resource_record_name
      record = option.resource_record_value
      type   = option.resource_record_type
    }
  }

  zone_id = data.terraform_remote_state.foundation.outputs.public_hosted_zone_id
  name    = each.value.name
  type    = each.value.type
  ttl     = 60
  records = [each.value.record]
}

resource "aws_acm_certificate_validation" "api" {
  certificate_arn         = aws_acm_certificate.api.arn
  validation_record_fqdns = [for record in aws_route53_record.api_certificate_validation : record.fqdn]
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.application.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.api.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.application.arn
  }
}

resource "aws_lb_listener" "http_redirect" {
  load_balancer_arn = aws_lb.application.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_route53_record" "api" {
  zone_id = data.terraform_remote_state.foundation.outputs.public_hosted_zone_id
  name    = var.api_domain_name
  type    = "A"

  alias {
    name                   = aws_lb.application.dns_name
    zone_id                = aws_lb.application.zone_id
    evaluate_target_health = true
  }
}
