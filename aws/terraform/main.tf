terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "FastTicket"
      Environment = "production"
      ManagedBy   = "Terraform"
    }
  }
}

# Data source para LabRole (pre-existente en AWS Labs)
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# ============================================================================
# VPC SIMPLIFICADA (Solo subnets públicas)
# ============================================================================

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = {
    Name = "fasticket-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  
  tags = {
    Name = "fasticket-igw"
  }
}

# Subnets públicas (2 para RDS que requiere multi-AZ)
resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true
  
  tags = {
    Name = "fasticket-subnet-a"
  }
}

resource "aws_subnet" "public_b" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.aws_region}b"
  map_public_ip_on_launch = true
  
  tags = {
    Name = "fasticket-subnet-b"
  }
}

# Route Table para subnets públicas
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
  
  tags = {
    Name = "fasticket-rt"
  }
}

resource "aws_route_table_association" "public_a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "public_b" {
  subnet_id      = aws_subnet.public_b.id
  route_table_id = aws_route_table.public.id
}

# ============================================================================
# SECURITY GROUPS
# ============================================================================

# Security Group para ECS Tasks (Backend)
resource "aws_security_group" "backend" {
  name_prefix = "fasticket-backend-"
  vpc_id      = aws_vpc.main.id
  description = "Security group for FastTicket backend"
  
  # Permitir acceso HTTP desde Internet (ya que no hay ALB)
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow HTTP access to backend"
  }
  
  # Permitir todo el tráfico de salida
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = "fasticket-backend-sg"
  }
}

# Security Group para RDS
resource "aws_security_group" "rds" {
  name_prefix = "fasticket-rds-"
  vpc_id      = aws_vpc.main.id
  description = "Security group for RDS PostgreSQL"
  
  # Permitir acceso desde el backend
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
    description     = "Allow access from backend"
  }
  
  # Acceso temporal desde Internet para gestión (opcional - comentar en producción real)
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Temporary access for management - REMOVE in production"
  }
  
  tags = {
    Name = "fasticket-rds-sg"
  }
}

# ============================================================================
# RDS POSTGRESQL (SIMPLIFICADO)
# ============================================================================

resource "aws_db_subnet_group" "main" {
  name       = "fasticket-db-subnet"
  subnet_ids = [aws_subnet.public_a.id, aws_subnet.public_b.id]
  
  tags = {
    Name = "fasticket-db-subnet"
  }
}

resource "aws_db_instance" "postgres" {
  identifier     = "fasticket-db"
  engine         = "postgres"
  engine_version = "16.4"
  instance_class = "db.t3.micro"
  
  db_name  = "fasticket"
  username = var.db_username
  password = var.db_password
  
  allocated_storage     = 20
  max_allocated_storage = 50
  storage_encrypted     = true
  
  multi_az               = false  # Single-AZ para reducir costos
  publicly_accessible    = true   # Para permitir conexión directa
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  
  backup_retention_period = 3
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"
  
  skip_final_snapshot      = true
  delete_automated_backups = true
  
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  
  tags = {
    Name = "fasticket-db"
  }
}

# ============================================================================
# ECR REPOSITORY
# ============================================================================

resource "aws_ecr_repository" "backend" {
  name                 = "fasticket-backend"
  image_tag_mutability = "MUTABLE"
  force_delete         = true  # Permite eliminar aunque contenga imágenes
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  tags = {
    Name = "fasticket-ecr"
  }
}

# ============================================================================
# ECS CLUSTER
# ============================================================================

resource "aws_ecs_cluster" "main" {
  name = "fasticket-cluster"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
  
  tags = {
    Name = "fasticket-cluster"
  }
}

# ============================================================================
# CLOUDWATCH LOGS
# ============================================================================

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/fasticket"
  retention_in_days = 7
  
  tags = {
    Name = "fasticket-logs"
  }
}

# ============================================================================
# ECS TASK DEFINITION
# ============================================================================

resource "aws_ecs_task_definition" "backend" {
  family                   = "fasticket-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  
  execution_role_arn = data.aws_iam_role.lab_role.arn
  task_role_arn      = data.aws_iam_role.lab_role.arn
  
  container_definitions = jsonencode([
    {
      name  = "fasticket-backend"
      image = "${aws_ecr_repository.backend.repository_url}:latest"
      
      essential = true
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
          hostPort      = 8080
        }
      ]
      
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "DB_HOST", value = aws_db_instance.postgres.address },
        { name = "DB_PORT", value = "5432" },
        { name = "DB_NAME", value = "fasticket" },
        { name = "DB_USERNAME", value = var.db_username },
        { name = "DB_PASSWORD", value = var.db_password },
        { name = "FRONTEND_URL", value = var.frontend_url },
        { name = "SWAGGER_ENABLED", value = var.swagger_enabled },
        { name = "JAVA_OPTS", value = "-Xmx768m -Xms512m -XX:+UseG1GC" }
      ]
      
      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
      
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
  
  tags = {
    Name = "fasticket-task"
  }
}

# ============================================================================
# ECS SERVICE (SIN LOAD BALANCER - IP PÚBLICA DIRECTA)
# ============================================================================

resource "aws_ecs_service" "backend" {
  name            = "fasticket-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  
  deployment_minimum_healthy_percent = 0   # Permite detener la tarea antes de iniciar otra
  deployment_maximum_percent         = 100  # Solo una tarea a la vez
  
  network_configuration {
    subnets          = [aws_subnet.public_a.id, aws_subnet.public_b.id]
    security_groups  = [aws_security_group.backend.id]
    assign_public_ip = true  # IMPORTANTE: Asignar IP pública para acceso directo
  }
  
  tags = {
    Name = "fasticket-service"
  }
}

# ============================================================================
# OUTPUTS
# ============================================================================

output "rds_endpoint" {
  description = "Endpoint de la base de datos RDS"
  value       = "${aws_db_instance.postgres.address}:${aws_db_instance.postgres.port}"
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecs_cluster_name" {
  description = "Nombre del cluster ECS"
  value       = aws_ecs_cluster.main.name
}

output "api_access_info" {
  description = "Información de acceso a la API"
  value       = "La API estará disponible en la IP pública de la tarea ECS en el puerto 8080. Consulta la consola de ECS para obtener la IP."
}

# Para obtener la IP pública de la tarea ECS después del despliegue:
# aws ecs list-tasks --cluster fasticket-cluster --service-name fasticket-service --region us-east-1
# aws ecs describe-tasks --cluster fasticket-cluster --tasks <task-id> --region us-east-1
