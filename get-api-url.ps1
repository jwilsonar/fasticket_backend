# Script para obtener la URL de la API de FastTicket en AWS

Write-Host "Buscando la tarea ECS de FastTicket..." -ForegroundColor Cyan

# Obtener el ARN de la tarea
$taskArn = aws ecs list-tasks --cluster fasticket-cluster --service-name fasticket-service --region us-east-1 --query 'taskArns[0]' --output text

if ([string]::IsNullOrEmpty($taskArn) -or $taskArn -eq "None") {
    Write-Host "No se encontro ninguna tarea en ejecucion" -ForegroundColor Red
    exit 1
}

Write-Host "Tarea encontrada: $taskArn" -ForegroundColor Green

# Obtener los detalles de la tarea
Write-Host "Obteniendo detalles de la tarea..." -ForegroundColor Cyan
$taskDetails = aws ecs describe-tasks --cluster fasticket-cluster --tasks $taskArn --region us-east-1 | ConvertFrom-Json

# Verificar el estado de la tarea
$taskStatus = $taskDetails.tasks[0].lastStatus
Write-Host "Estado de la tarea: $taskStatus" -ForegroundColor Yellow

if ($taskStatus -ne "RUNNING") {
    Write-Host "La tarea aun no esta en estado RUNNING" -ForegroundColor Yellow
    Write-Host "Espera unos momentos y vuelve a ejecutar este script" -ForegroundColor Yellow
    exit 0
}

# Obtener la ENI ID
$eniId = ($taskDetails.tasks[0].attachments[0].details | Where-Object { $_.name -eq "networkInterfaceId" }).value

if ([string]::IsNullOrEmpty($eniId)) {
    Write-Host "No se pudo obtener la interfaz de red" -ForegroundColor Red
    exit 1
}

Write-Host "Interfaz de red: $eniId" -ForegroundColor Green

# Obtener la IP pública
Write-Host "Obteniendo IP publica..." -ForegroundColor Cyan
$publicIp = aws ec2 describe-network-interfaces --network-interface-ids $eniId --region us-east-1 --query 'NetworkInterfaces[0].Association.PublicIp' --output text

if ([string]::IsNullOrEmpty($publicIp) -or $publicIp -eq "None") {
    Write-Host "No se pudo obtener la IP publica" -ForegroundColor Red
    Write-Host "La tarea puede estar iniciando todavia" -ForegroundColor Yellow
    exit 1
}

# Mostrar información
Write-Host ""
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "FastTicket API - Informacion de Acceso" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "IP Publica:        " -NoNewline -ForegroundColor Cyan
Write-Host $publicIp -ForegroundColor White
Write-Host ""
Write-Host "API Base URL:      " -NoNewline -ForegroundColor Cyan
Write-Host "http://$publicIp:8080" -ForegroundColor White
Write-Host ""
Write-Host "Health Check:      " -NoNewline -ForegroundColor Cyan
Write-Host "http://$publicIp:8080/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "Swagger UI:        " -NoNewline -ForegroundColor Cyan
Write-Host "http://$publicIp:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host ""
Write-Host "API Docs:          " -NoNewline -ForegroundColor Cyan
Write-Host "http://$publicIp:8080/v3/api-docs" -ForegroundColor White
Write-Host ""
Write-Host "======================================================================" -ForegroundColor Green
Write-Host ""

# Probar conectividad
Write-Host "Probando conectividad..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://$publicIp:8080/actuator/health" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "La API esta ONLINE y responde correctamente!" -ForegroundColor Green
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Gray
} catch {
    Write-Host "No se pudo conectar a la API todavia" -ForegroundColor Yellow
    Write-Host "La aplicacion puede estar iniciando (tarda 1-2 minutos)" -ForegroundColor Yellow
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Intenta de nuevo en unos momentos" -ForegroundColor Cyan
}

Write-Host ""
