resource "aws_secretsmanager_secret" "application_database" {
  name                    = "${var.project_name}/${var.environment}/database/application"
  description             = "Runtime credential for the dedicated NitrosGames production application database user."
  recovery_window_in_days = 30

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Name    = "${var.project_name}/${var.environment}/database/application"
    Purpose = "application-database-credential"
  }
}
