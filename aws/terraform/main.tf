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
# VPC SIMPLIFICADA (Solo subnets públicas para reducir costos)
# ============================================================================

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = {
    Name = "fasticket-vpc-prod"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  
  tags = {
    Name = "fasticket-igw-prod"
  }
}

# Subnets públicas (2 para alta disponibilidad del ALB)
resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true
  
  tags = {
    Name = "fasticket-public-a"
  }
}

resource "aws_subnet" "public_b" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.aws_region}b"
  map_public_ip_on_launch = true
  
  tags = {
    Name = "fasticket-public-b"
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
    Name = "fasticket-public-rt"
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

# Security Group para ALB
resource "aws_security_group" "alb" {
  name_prefix = "fasticket-alb-"
  vpc_id      = aws_vpc.main.id
  description = "Security group for ALB"
  
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = "fasticket-alb-sg"
  }
}

# Security Group para ECS Tasks
resource "aws_security_group" "ecs_tasks" {
  name_prefix = "fasticket-ecs-"
  vpc_id      = aws_vpc.main.id
  description = "Security group for ECS tasks"
  
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = "fasticket-ecs-sg"
  }
}

# Security Group para RDS
resource "aws_security_group" "rds" {
  name_prefix = "fasticket-rds-"
  vpc_id      = aws_vpc.main.id
  description = "Security group for RDS"
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
  
  # Acceso temporal desde cualquier IP para pruebas
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Acceso temporal para desarrollo - CAMBIAR en producción real"
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = "fasticket-rds-sg"
  }
}

# ============================================================================
# RDS POSTGRESQL (SIMPLIFICADO - Single-AZ para reducir costos)
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
  instance_class = "db.t3.micro"  # Cambiado a micro para reducir costos
  
  db_name  = "fasticket"
  username = var.db_username
  password = var.db_password
  
  allocated_storage     = 20
  max_allocated_storage = 50  # Reducido de 100 a 50
  storage_encrypted     = true
  
  multi_az               = false  # Single-AZ para reducir costos
  publicly_accessible    = true   # Permite conexión directa para pruebas
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  
  backup_retention_period = 3  # Reducido de 7 a 3 días
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"
  
  skip_final_snapshot       = true  # Para facilitar destrucción en desarrollo
  delete_automated_backups  = true
  
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
  force_delete         = true  # Permite eliminar el repositorio aunque contenga imágenes
  
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
# APPLICATION LOAD BALANCER
# ============================================================================

resource "aws_lb" "main" {
  name               = "fasticket-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [aws_subnet.public_a.id, aws_subnet.public_b.id]
  
  enable_deletion_protection = false
  
  tags = {
    Name = "fasticket-alb"
  }
}

resource "aws_lb_target_group" "backend" {
  name        = "fasticket-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"
  
  deregistration_delay = 30
  
  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 3
  }
  
  tags = {
    Name = "fasticket-tg"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"
  
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

# ============================================================================
# CLOUDWATCH LOGS
# ============================================================================

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/fasticket"
  retention_in_days = 7  # Reducido de 30 a 7 días
  
  tags = {
    Name = "fasticket-logs"
  }
}

# ============================================================================
# ECS TASK DEFINITION (SIN REDIS)
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
      image = "${aws_ecr_repository.backend.repository_url}:prod-latest"
      
      essential = true
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
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
# ECS SERVICE
# ============================================================================

resource "aws_ecs_service" "backend" {
  name            = "fasticket-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 1  # Reducido de 2 a 1 para minimizar costos
  launch_type     = "FARGATE"
  
  deployment_minimum_healthy_percent = 50  # Reducido de 100 a 50
  deployment_maximum_percent         = 200
  
  force_new_deployment = false
  
  network_configuration {
    subnets          = [aws_subnet.public_a.id, aws_subnet.public_b.id]
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = true  # IMPORTANTE: Necesario en subnets públicas
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "fasticket-backend"
    container_port   = 8080
  }
  
  depends_on = [aws_lb_listener.http]
  
  tags = {
    Name = "fasticket-service"
  }
}

# ============================================================================
# OUTPUTS
# ============================================================================

output "alb_dns_name" {
  description = "DNS del Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "rds_endpoint" {
  description = "Endpoint de la base de datos RDS"
  value       = "${aws_db_instance.postgres.address}:${aws_db_instance.postgres.port}"
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR"
  value       = aws_ecr_repository.backend.repository_url
}

output "api_url" {
  description = "URL base de la API"
  value       = "http://${aws_lb.main.dns_name}"
}

output "swagger_url" {
  description = "URL de Swagger UI"
  value       = "http://${aws_lb.main.dns_name}/swagger-ui/index.html"
}
