# FastTicket - Makefile para Gestión del Sistema
# 
# Este Makefile automatiza la gestión del stack completo de FastTicket:
# - PostgreSQL 16 (Base de datos principal)
# - Redis 7 (Caché y sesiones)
# - Spring Boot (Backend API REST)
#
# REQUISITOS:
#   - Docker Desktop instalado y ejecutándose
#   - Make (incluido en Git Bash para Windows)
#   - Maven (para ejecución local sin Docker)
#   - Java 21 (para ejecución local sin Docker)
#
# INICIO RÁPIDO:
#   make check-all    # Verificar dependencias
#   make build        # Construir imágenes Docker
#   make up           # Iniciar todos los servicios
#   make logs-backend # Ver logs del backend
#   make down         # Detener servicios
#
# PUERTOS:
#   - Backend:    http://localhost:8081
#   - PostgreSQL: localhost:5432
#   - Redis:      localhost:6379
#
# ==============================================================================

.PHONY: help build up down restart logs logs-backend logs-db logs-redis clean rebuild status health shell-backend shell-db test docker-check check-docker check-maven check-java check-all run-local package install

COMPOSE = docker-compose
BACKEND_SERVICE = backend
REDIS_SERVICE = redis
DB_SERVICE = postgres

docker-check:
	@docker info >nul 2>&1 || (echo [ERROR] Docker Desktop no esta corriendo. Inicia Docker Desktop y vuelve a intentar. && exit 1)

help:
	@echo ================================================================
	@echo   FastTicket - Sistema de Gestion de Tickets
	@echo ================================================================
	@cmd /c echo.
	@echo INICIO RAPIDO:
	@echo   make check-all     Verificar dependencias
	@echo   make build         Construir imagenes
	@echo   make up            Iniciar servicios
	@echo   make logs-backend  Ver logs
	@cmd /c echo.
	@echo DOCKER:
	@echo   build rebuild up down restart status
	@cmd /c echo.
	@echo LOGS Y MONITOREO:
	@echo   logs logs-backend logs-db logs-redis health urls
	@cmd /c echo.
	@echo SHELL ACCESO:
	@echo   shell-backend shell-db shell-redis
	@cmd /c echo.
	@echo DESARROLLO:
	@echo   dev watch run-local package install compile test
	@cmd /c echo.
	@echo BASE DE DATOS:
	@echo   db-backup db-restore db-connect db-reset
	@cmd /c echo.
	@echo LIMPIEZA:
	@echo   clean clean-volumes clean-all
	@cmd /c echo.
	@echo ================================================================

# DOCKER - Comandos principales

build: docker-check
	@echo [*] Construyendo imagenes...
	$(COMPOSE) build
	@echo [OK] Imagenes construidas

up: docker-check
	@echo [*] Iniciando servicios (PostgreSQL + Redis + Backend)...
	$(COMPOSE) up -d
	@echo [OK] Servicios iniciados
	@cmd /c echo.
	@$(MAKE) status

start: up

down:
	@echo [*] Deteniendo servicios...
	$(COMPOSE) down
	@echo [OK] Servicios detenidos

stop: down

restart:
	@$(MAKE) down
	@$(MAKE) up

restart-backend:
	@echo [*] Reiniciando backend...
	$(COMPOSE) restart $(BACKEND_SERVICE)
	@echo [OK] Backend reiniciado

rebuild: docker-check
	@echo [*] Reconstruyendo todo...
	$(COMPOSE) up --build -d
	@$(MAKE) status

# LOGS Y MONITOREO

logs:
	$(COMPOSE) logs -f

logs-backend:
	$(COMPOSE) logs -f $(BACKEND_SERVICE)

logs-db:
	$(COMPOSE) logs -f $(DB_SERVICE)

logs-redis:
	$(COMPOSE) logs -f $(REDIS_SERVICE)

status:
	@echo [*] Estado de los servicios:
	@$(COMPOSE) ps

health:
	@echo [*] Verificando salud del backend...
	@curl -s http://localhost:8081/actuator/health || echo [ERROR] Backend no esta respondiendo

urls:
	@echo [*] URLs disponibles:
	@echo   Backend API:      http://localhost:8081
	@echo   Health Check:     http://localhost:8081/actuator/health
	@echo   PostgreSQL:       localhost:5432 (usuario: postgres, db: fasticket)
	@echo   Redis:            localhost:6379

# SHELL - Acceso a contenedores

shell-backend:
	$(COMPOSE) exec $(BACKEND_SERVICE) sh

shell-db:
	$(COMPOSE) exec $(DB_SERVICE) psql -U postgres -d fasticket

shell-redis:
	$(COMPOSE) exec $(REDIS_SERVICE) redis-cli

# DESARROLLO

dev:
	@$(MAKE) rebuild
	@$(MAKE) logs-backend

watch:
	@$(MAKE) logs-backend

run-local:
	@echo [*] Ejecutando localmente (requiere PostgreSQL y Redis activos)...
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

package:
	@echo [*] Empaquetando proyecto...
	mvn clean package
	@echo [OK] JAR creado en target\fasticket-0.0.1-SNAPSHOT.jar

install:
	mvn clean install

# BASE DE DATOS - PostgreSQL

db-backup:
	@echo [*] Creando backup...
	@if not exist backups mkdir backups
	@$(COMPOSE) exec -T $(DB_SERVICE) pg_dump -U postgres fasticket > backups\backup_fasticket_$(shell powershell -Command "Get-Date -Format 'yyyyMMdd_HHmmss'").sql
	@echo [OK] Backup creado en backups\

db-restore:
	@echo [!] Uso: make db-restore FILE=backups\backup_fasticket.sql
	@if exist "$(FILE)" ($(COMPOSE) exec -T $(DB_SERVICE) psql -U postgres -d fasticket < $(FILE) && echo [OK] Backup restaurado) else (echo [ERROR] Archivo no encontrado)

db-connect:
	@$(COMPOSE) exec $(DB_SERVICE) psql -U postgres -d fasticket

db-reset:
	@echo [!] CUIDADO: Eliminara todos los datos
	@$(COMPOSE) exec $(DB_SERVICE) psql -U postgres -c "DROP DATABASE IF EXISTS fasticket;"
	@$(COMPOSE) exec $(DB_SERVICE) psql -U postgres -c "CREATE DATABASE fasticket;"
	@echo [OK] Base de datos reiniciada

# LIMPIEZA

clean:
	$(COMPOSE) down

clean-all:
	@echo [!] CUIDADO: Eliminara TODOS los datos y volumenes
	$(COMPOSE) down -v --rmi all

clean-volumes:
	@echo [!] CUIDADO: Eliminara volumenes de datos
	$(COMPOSE) down -v

# TESTING Y VALIDACION

test:
	mvn test

compile:
	mvn clean compile

validate:
	$(COMPOSE) config

# VERIFICACION DE DEPENDENCIAS

check-docker:
	@echo [*] Verificando Docker...
	@docker --version || echo [ERROR] Docker no instalado
	@docker-compose --version || echo [ERROR] Docker Compose no instalado
	@docker info >nul 2>&1 && echo [OK] Docker corriendo || echo [ERROR] Docker NO esta corriendo

check-maven:
	@mvn --version || echo [ERROR] Maven no instalado

check-java:
	@java -version || echo [ERROR] Java no instalado

check-all: check-java check-maven check-docker

info: docker-check
	@docker --version
	@docker-compose --version
	@$(COMPOSE) exec $(BACKEND_SERVICE) java -version 2>&1 || echo Backend no esta corriendo

ps: status
