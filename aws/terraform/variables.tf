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
  default     = "https://fasticket-proyect.netlify.app/"
}

variable "swagger_enabled" {
  description = "Habilitar Swagger UI y API Docs en producción (true/false)"
  type        = string
  default     = "true"
}

variable "s3_bucket_prefix" {
  description = "Prefijo para el nombre del bucket S3 (se añadirá un sufijo aleatorio)"
  type        = string
  default     = "fasticket-images"
}

variable "brevo_enabled" {
  description = "Habilitar proveedor de emails Brevo en la app (true/false)"
  type        = string
  default     = "true"
}

variable "brevo_api_key" {
  description = "API Key de Brevo (no commitear, usar variables de entorno/secretos)"
  type        = string
  sensitive   = true
}

variable "brevo_sender_email" {
  description = "Email remitente verificado en Brevo"
  type        = string
  default     = "noreply@fasticket.com"
}

variable "brevo_sender_name" {
  description = "Nombre del remitente para correos Brevo"
  type        = string
  default     = "Fasticket"
}

variable "brevo_api_url" {
  description = "Endpoint SMTP/API de Brevo"
  type        = string
  default     = "https://api.brevo.com/v3/smtp/email"
}

