variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "fasticket_admin"
  sensitive   = true
}

variable "db_password" {
  description = "Database master password (min 8 caracteres)"
  type        = string
  sensitive   = true
}

variable "frontend_url" {
  description = "Frontend URL para CORS (ej: https://fasticket.com)"
  type        = string
  default     = "https://fasticket.com"
}

variable "swagger_enabled" {
  description = "Habilitar Swagger UI y API Docs en producción (true/false)"
  type        = string
  default     = "false"
}

