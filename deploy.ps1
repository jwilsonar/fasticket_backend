Write-Host "[*] Iniciando despliegue en AWS..." -ForegroundColor Green
Write-Host "`n[1] Verificando credenciales..." -ForegroundColor Yellow
aws sts get-caller-identity
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Credenciales invalidas o expiradas" -ForegroundColor Red
    Write-Host "Ejecuta: aws configure" -ForegroundColor Yellow
    Write-Host "Y luego: `$env:AWS_SESSION_TOKEN='<token>'" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n[2] Obteniendo URL de ECR..." -ForegroundColor Yellow
Push-Location aws\terraform
$ECR_URL = terraform output -raw ecr_repository_url
Write-Host "ECR: $ECR_URL" -ForegroundColor Cyan
Pop-Location

Write-Host "`n[3] Autenticando en ECR..." -ForegroundColor Yellow
$password = (aws ecr get-login-password --region us-east-1)
docker login -u AWS -p $password $ECR_URL

Write-Host "`n[4] Construyendo imagen Docker..." -ForegroundColor Yellow
docker build -t fasticket-backend:prod .
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Error en build de Docker" -ForegroundColor Red
    exit 1
}

Write-Host "`n[5] Subiendo imagen a ECR..." -ForegroundColor Yellow
$IMAGE_NAME = "${ECR_URL}:prod-latest"
docker tag fasticket-backend:prod $IMAGE_NAME
docker push $IMAGE_NAME
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Error al subir imagen" -ForegroundColor Red
    exit 1
}

Write-Host "`n[6] Actualizando servicio ECS..." -ForegroundColor Yellow
aws ecs update-service `
    --cluster fasticket-cluster `
    --service fasticket-service-prod `
    --force-new-deployment `
    --region us-east-1 `
    --no-cli-pager

Write-Host "`n[7] Estado del servicio:" -ForegroundColor Yellow
aws ecs describe-services `
    --cluster fasticket-cluster `
    --services fasticket-service-prod `
    --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount}' `
    --region us-east-1

Write-Host "`n[OK] Despliegue completado!" -ForegroundColor Green
Write-Host "`n[*] Verifica el estado en:" -ForegroundColor Cyan
Push-Location aws\terraform
$ALB_URL = terraform output -raw alb_dns_name
Pop-Location
Write-Host "http://$ALB_URL/actuator/health" -ForegroundColor Blue

Write-Host "`n[*] Esperando a que el servicio se estabilice (esto puede tomar 2-3 minutos)..." -ForegroundColor Yellow
Write-Host "Puedes cancelar con Ctrl+C y verificar manualmente despues." -ForegroundColor Gray

$maxAttempts = 30
$attempt = 0
$stable = $false

while ($attempt -lt $maxAttempts -and -not $stable) {
    Start-Sleep -Seconds 10
    $attempt++
    
    $serviceInfo = aws ecs describe-services `
        --cluster fasticket-cluster `
        --services fasticket-service-prod `
        --query 'services[0].{Running:runningCount,Desired:desiredCount}' `
        --region us-east-1 | ConvertFrom-Json
    
    $running = $serviceInfo.Running
    $desired = $serviceInfo.Desired
    
    Write-Host "  Intento $attempt/$maxAttempts - Running: $running/$desired" -ForegroundColor Gray
    
    if ($running -eq $desired -and $running -gt 0) {
        $stable = $true
    }
}

if ($stable) {
    Write-Host "`n[OK] Servicio estable y funcionando!" -ForegroundColor Green
    Write-Host "`nPrueba la aplicacion:" -ForegroundColor Cyan
    Write-Host "  curl http://$ALB_URL/actuator/health" -ForegroundColor Blue
} else {
    Write-Host "`n[!] El servicio aun no esta completamente estable." -ForegroundColor Yellow
    Write-Host "Verifica el estado manualmente en unos minutos." -ForegroundColor Gray
}
