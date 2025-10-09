terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
  
  backend "s3" {
    bucket = "fasticket-terraform-state-134044682888383528"
    key    = "prod/terraform.tfstate"
    region = "us-east-1"
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

# VPC y Networking
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  
  name = "fasticket-vpc-prod"
  cidr = "10.0.0.0/16"
  
  azs             = ["us-east-1a", "us-east-1b"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24"]
  
  enable_nat_gateway = true
  single_nat_gateway = false  # 2 NAT Gateways para alta disponibilidad
  
  enable_dns_hostnames = true
  enable_dns_support   = true
}

# Security Groups
resource "aws_security_group" "alb" {
  name_prefix = "fasticket-alb-prod"
  vpc_id      = module.vpc.vpc_id
  
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
}

resource "aws_security_group" "ecs_tasks" {
  name_prefix = "fasticket-ecs-prod"
  vpc_id      = module.vpc.vpc_id
  
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
}

resource "aws_security_group" "rds" {
  name_prefix = "fasticket-rds-prod"
  vpc_id      = module.vpc.vpc_id
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}

resource "aws_security_group" "redis" {
  name_prefix = "fasticket-redis-prod"
  vpc_id      = module.vpc.vpc_id
  
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  identifier     = "fasticket-db-prod"
  engine         = "postgres"
  engine_version = "16.4"
  instance_class = "db.t3.small"
  
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_encrypted     = true
  
  db_name  = "fasticket"
  username = var.db_username
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"
  
  skip_final_snapshot       = false
  final_snapshot_identifier = "fasticket-db-final-snapshot"
  
  multi_az            = true
  publicly_accessible = false
  
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
}

resource "aws_db_subnet_group" "main" {
  name       = "fasticket-db-subnet-prod"
  subnet_ids = module.vpc.private_subnets
}

# ElastiCache Redis
resource "aws_elasticache_subnet_group" "main" {
  name       = "fasticket-redis-subnet-prod"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "fasticket-redis-prod"
  description          = "FastTicket Redis Cluster Production"
  
  engine         = "redis"
  engine_version = "7.0"
  node_type      = "cache.t3.small"
  
  num_cache_clusters = 2
  
  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = false
  
  automatic_failover_enabled = true
  
  snapshot_retention_limit = 5
  snapshot_window         = "03:00-05:00"
}

# ECR Repository
resource "aws_ecr_repository" "backend" {
  name = "fasticket-backend"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  image_tag_mutability = "MUTABLE"
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "fasticket-cluster"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "fasticket-alb-prod"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = module.vpc.public_subnets
  
  enable_deletion_protection = false
  enable_http2              = true
}

resource "aws_lb_target_group" "backend" {
  name        = "fasticket-tg-prod"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
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

# ECS Task Definition
resource "aws_ecs_task_definition" "backend" {
  family                   = "fasticket-task-prod"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  
  execution_role_arn = aws_iam_role.ecs_execution.arn
  task_role_arn      = aws_iam_role.ecs_task.arn
  
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
        { name = "REDIS_HOST", value = aws_elasticache_replication_group.redis.primary_endpoint_address },
        { name = "REDIS_PORT", value = "6379" },
        { name = "FRONTEND_URL", value = var.frontend_url },
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
}

# ECS Service
resource "aws_ecs_service" "backend" {
  name            = "fasticket-service-prod"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 2
  launch_type     = "FARGATE"
  
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  
  network_configuration {
    security_groups  = [aws_security_group.ecs_tasks.id]
    subnets          = module.vpc.private_subnets
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "fasticket-backend"
    container_port   = 8080
  }
  
  depends_on = [aws_lb_listener.http]
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/fasticket"
  retention_in_days = 30
}

# IAM Roles
resource "aws_iam_role" "ecs_execution" {
  name = "fasticket-ecs-execution-prod"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_task" {
  name = "fasticket-ecs-task-prod"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

# Outputs
output "alb_dns_name" {
  value = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "rds_endpoint" {
  value = aws_db_instance.postgres.endpoint
}

output "redis_endpoint" {
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
}

