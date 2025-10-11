# Script de Despliegue Rapido - FastTicket
# Uso: .\deploy-quick.ps1
# Para cambios menores sin rebuild completo

$ErrorActionPreference = "Stop"

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "DESPLIEGUE RAPIDO - FASTICKET" -ForegroundColor Cyan
Write-Host "================================================================`n" -ForegroundColor Cyan

# 1. Build de imagen
Write-Host "[1/5] Construyendo imagen Docker..." -ForegroundColor Yellow
docker build -t fasticket-backend:prod . --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error al construir imagen" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] Imagen construida" -ForegroundColor Green

# 2. Obtener ECR URL
Write-Host "`n[2/5] Obteniendo configuracion..." -ForegroundColor Yellow
Push-Location aws\terraform
$ECR_URL = terraform output -raw ecr_repository_url 2>$null
Pop-Location

if (-not $ECR_URL) {
    Write-Host "Error: No se pudo obtener ECR URL de Terraform" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] ECR: $ECR_URL" -ForegroundColor Green

# 3. Autenticar con ECR
Write-Host "`n[3/5] Autenticando con ECR..." -ForegroundColor Yellow
$ECR_REGISTRY = $ECR_URL.Split('/')[0]
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_REGISTRY 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Autenticado" -ForegroundColor Green
} else {
    Write-Host "  [!] Error de autenticacion" -ForegroundColor Yellow
}

# 4. Push de imagen
Write-Host "`n[4/5] Subiendo imagen a ECR..." -ForegroundColor Yellow
docker tag fasticket-backend:prod "${ECR_URL}:latest"
docker push "${ECR_URL}:latest" --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error al subir imagen" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] Imagen subida" -ForegroundColor Green

# 5. Actualizar servicio ECS
Write-Host "`n[5/5] Actualizando servicio ECS..." -ForegroundColor Yellow
aws ecs update-service --cluster fasticket-cluster --service fasticket-service --force-new-deployment --region us-east-1 --no-cli-pager | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error al actualizar servicio" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] Servicio actualizado" -ForegroundColor Green

Write-Host "`n================================================================" -ForegroundColor Green
Write-Host "DESPLIEGUE INICIADO" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host "`nLa aplicacion tardara 1-2 minutos en estar disponible" -ForegroundColor Yellow
Write-Host "Ejecuta: .\get-api-url.ps1 para obtener la IP publica`n" -ForegroundColor Cyan

