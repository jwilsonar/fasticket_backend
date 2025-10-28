# FastTicket

Sistema de venta de tickets y gestión de eventos desarrollado con Spring Boot.

## Tecnologías

- **Backend**: Spring Boot 3.5.6
- **Base de datos**: PostgreSQL 16
- **Cache**: Redis 7
- **Containerización**: Docker
- **Despliegue**: AWS (ECS, RDS, ElastiCache)
- **Infraestructura**: Terraform

## Requisitos Previos

- Java 21
- Maven 3.x
- Docker y Docker Compose
- AWS CLI (para despliegue)
- Terraform (para infraestructura)

## Arquitectura

El proyecto utiliza una arquitectura de microservicios desplegada en AWS:

- **ECS Fargate**: Contenedores de aplicación
- **RDS PostgreSQL**: Base de datos principal
- **ElastiCache Redis**: Sistema de cache
- **Application Load Balancer**: Balanceo de carga
- **ECR**: Registro de imágenes Docker

## Desarrollo Local

### Con Docker Compose

```bash
# Levantar servicios
docker-compose up -d

# Ver logs
docker-compose logs -f

# Detener servicios
docker-compose down
```

### Sin Docker

```bash
# Instalar dependencias
mvn clean install

# Ejecutar aplicación
mvn spring-boot:run
```

## Estructura del Proyecto

```
fasticket/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── pe/edu/pucp/fasticket/
│   │   │       ├── controllers/      # Controladores REST
│   │   │       ├── dto/              # Data Transfer Objects
│   │   │       ├── model/            # Entidades JPA
│   │   │       ├── repository/       # Repositorios JPA
│   │   │       └── services/         # Lógica de negocio
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/
├── aws/
│   └── terraform/                    # Infraestructura como código
├── docker-compose.yml
├── Dockerfile
└── pom.xml 
```

## Configuración

### Variables de Entorno

```env
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fasticket
DB_USERNAME=postgres
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Aplicación
SPRING_PROFILES_ACTIVE=dev
```

## API Endpoints

### Health Check
```
GET /actuator/health
```

### Personas
```
GET    /api/personas
POST   /api/personas
GET    /api/personas/{id}
PUT    /api/personas/{id}
DELETE /api/personas/{id}
```

