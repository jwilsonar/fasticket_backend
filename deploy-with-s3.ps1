# Script de Despliegue Fasticket en AWS ECS con S3 (Configuracion Simplificada)
param(
    [switch]$Clean = $false
)

$ErrorActionPreference = "Stop"
$AWS_REGION = "us-east-1"
$CLUSTER_NAME = "fasticket-cluster"
$SERVICE_NAME = "fasticket-service"
$LOG_GROUP = "/ecs/fasticket"

function Write-Step {
    param([string]$Message, [int]$Step)
    Write-Host "`n[$Step] $Message" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "  [OK] $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "  $Message" -ForegroundColor Cyan
}

function Write-Error-Message {
    param([string]$Message)
    Write-Host "`n[ERROR] $Message" -ForegroundColor Red
}

function Exit-WithError {
    param([string]$Message)
    Write-Error-Message $Message
    exit 1
}

function Test-AWSCredentials {
    Write-Step "Verificando credenciales de AWS..." 1
    try {
        $identity = aws sts get-caller-identity 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Credenciales invalidas"
        }
        Write-Success "Credenciales validas"
        return $true
    } catch {
        Exit-WithError "Credenciales de AWS invalidas o expiradas"
    }
}

function Test-DockerRunning {
    try {
        docker info 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Exit-WithError "Docker no esta en ejecucion"
        }
    } catch {
        Exit-WithError "Docker no esta disponible"
    }
}

function Invoke-CleanBuild {
    Write-Host "`nMODO LIMPIO ACTIVADO" -ForegroundColor Magenta
    
    Write-Step "Limpiando build de Maven..." 2
    try {
        mvn clean -q
        Remove-Item -Path "target" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Success "Maven limpiado"
    } catch {
        Write-Host "  [!] No se pudo limpiar Maven completamente" -ForegroundColor Yellow
    }
    
    Write-Step "Limpiando cache de Docker..." 3
    docker builder prune -af 2>&1 | Out-Null
    Write-Success "Cache de Docker eliminado"
    
    Write-Step "Eliminando imagenes locales antiguas..." 4
    docker rmi fasticket-backend:prod -f 2>$null
    $images = docker images --filter=reference="fasticket-backend*" -q
    if ($images) {
        $images | ForEach-Object { docker rmi $_ -f 2>$null }
    }
    Write-Success "Imagenes locales eliminadas"
}

function Get-TerraformOutput {
    param([string]$OutputName)
    
    Push-Location aws\terraform
    try {
        $value = terraform output -raw $OutputName 2>$null
        if (-not $value -or $LASTEXITCODE -ne 0) {
            Pop-Location
            Exit-WithError "No se pudo obtener '$OutputName' de Terraform"
        }
        Pop-Location
        return $value
    } catch {
        Pop-Location
        Exit-WithError "Error al obtener configuracion de Terraform"
    }
}

function Build-DockerImage {
    param(
        [bool]$UseCache,
        [int]$StepNumber
    )
    
    Write-Step "Construyendo imagen Docker..." $StepNumber
    
    $TIMESTAMP = Get-Date -Format "yyyyMMdd-HHmmss"
    $GIT_HASH = git rev-parse --short HEAD 2>$null
    $TAG_VERSION = if ($GIT_HASH) { "${TIMESTAMP}-${GIT_HASH}" } else { $TIMESTAMP }
    
    Write-Info "Tag de version: $TAG_VERSION"
    Write-Info "Modo cache: $(if ($UseCache) { 'Activado' } else { 'Desactivado' })"
    
    $buildArgs = @(
        "build"
        if (-not $UseCache) { "--no-cache" }
        if (-not $UseCache) { "--pull" }
        "--progress=plain"
        "-t", "fasticket-backend:prod"
        "-t", "fasticket-backend:$TAG_VERSION"
        "."
    ) | Where-Object { $_ }
    
    & docker $buildArgs
    
    if ($LASTEXITCODE -ne 0) {
        Exit-WithError "Error durante la construccion de la imagen Docker"
    }
    
    Write-Success "Imagen construida exitosamente"
    return $TAG_VERSION
}

function Push-DockerImage {
    param(
        [string]$ECR_URL,
        [string]$TAG_VERSION,
        [int]$StepNumber
    )
    
    Write-Step "Subiendo imagen a ECR..." $StepNumber
    
    $IMAGE_NAME_LATEST = "${ECR_URL}:latest"
    $IMAGE_NAME_VERSION = "${ECR_URL}:${TAG_VERSION}"
    
    docker tag fasticket-backend:prod $IMAGE_NAME_LATEST
    docker tag fasticket-backend:prod $IMAGE_NAME_VERSION
    
    Write-Info "Subiendo: latest"
    docker push $IMAGE_NAME_LATEST | Out-Null
    
    Write-Info "Subiendo: $TAG_VERSION"
    docker push $IMAGE_NAME_VERSION | Out-Null
    
    if ($LASTEXITCODE -ne 0) {
        Exit-WithError "Error al subir la imagen a ECR"
    }
    
    Write-Success "Imagen subida exitosamente"
}

function Update-ECSService {
    param([int]$StepNumber)
    
    Write-Step "Actualizando servicio en ECS..." $StepNumber
    
    aws ecs update-service `
        --cluster $CLUSTER_NAME `
        --service $SERVICE_NAME `
        --force-new-deployment `
        --region $AWS_REGION `
        --no-cli-pager | Out-Null
    
    if ($LASTEXITCODE -ne 0) {
        Exit-WithError "Error al actualizar el servicio ECS"
    }
    
    Write-Success "Servicio actualizado"
}

function Get-ServiceStatus {
    param([int]$StepNumber)
    
    Write-Step "Estado del servicio:" $StepNumber
    
    $serviceInfo = aws ecs describe-services `
        --cluster $CLUSTER_NAME `
        --services $SERVICE_NAME `
        --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount,UpdatedAt:deployments[0].updatedAt}' `
        --region $AWS_REGION | ConvertFrom-Json
    
    Write-Host "  Status:  $($serviceInfo.Status)" -ForegroundColor White
    Write-Host "  Running: $($serviceInfo.Running)/$($serviceInfo.Desired)" -ForegroundColor White
    Write-Host "  Updated: $($serviceInfo.UpdatedAt)" -ForegroundColor White
}

function Get-TaskPublicIP {
    Write-Host "  Obteniendo IP publica de la tarea ECS..." -ForegroundColor Gray
    
    # Esperar un momento para que la tarea inicie
    Start-Sleep -Seconds 5
    
    $maxAttempts = 20
    $attempt = 0
    
    while ($attempt -lt $maxAttempts) {
        $attempt++
        
        try {
            $taskArn = aws ecs list-tasks `
                --cluster $CLUSTER_NAME `
                --service-name $SERVICE_NAME `
                --region $AWS_REGION `
                --query 'taskArns[0]' `
                --output text 2>$null
            
            if ($taskArn -and $taskArn -ne "None" -and $taskArn -ne "") {
                $taskDetails = aws ecs describe-tasks `
                    --cluster $CLUSTER_NAME `
                    --tasks $taskArn `
                    --region $AWS_REGION 2>$null | ConvertFrom-Json
                
                if ($taskDetails.tasks[0].lastStatus -eq "RUNNING") {
                    $eniId = ($taskDetails.tasks[0].attachments[0].details | Where-Object { $_.name -eq "networkInterfaceId" }).value
                    
                    if ($eniId) {
                        $publicIp = aws ec2 describe-network-interfaces `
                            --network-interface-ids $eniId `
                            --region $AWS_REGION `
                            --query 'NetworkInterfaces[0].Association.PublicIp' `
                            --output text 2>$null
                        
                        if ($publicIp -and $publicIp -ne "None" -and $publicIp -ne "") {
                            return $publicIp
                        }
                    }
                }
            }
            
            Write-Host "  Intento $attempt/$maxAttempts - Esperando que la tarea inicie..." -ForegroundColor Gray
            Start-Sleep -Seconds 10
            
        } catch {
            Write-Host "  Error al obtener IP, reintentando..." -ForegroundColor Yellow
            Start-Sleep -Seconds 5
        }
    }
    
    return $null
}

function Wait-ServiceStable {
    param(
        [string]$PublicIP,
        [int]$StepNumber
    )
    
    Write-Step "Esperando estabilizacion del servicio..." $StepNumber
    Write-Host "  Esto puede tomar 2-3 minutos" -ForegroundColor Gray
    
    $maxAttempts = 30
    $attempt = 0
    $stable = $false
    
    while ($attempt -lt $maxAttempts -and -not $stable) {
        Start-Sleep -Seconds 10
        $attempt++
        
        try {
            $serviceInfo = aws ecs describe-services `
                --cluster $CLUSTER_NAME `
                --services $SERVICE_NAME `
                --query 'services[0].{Running:runningCount,Desired:desiredCount}' `
                --region $AWS_REGION 2>$null | ConvertFrom-Json
            
            $running = $serviceInfo.Running
            $desired = $serviceInfo.Desired
            
            Write-Host "  Intento $attempt/$maxAttempts - Running: $running/$desired" -ForegroundColor Gray
            
            if ($running -eq $desired -and $running -gt 0) {
                $stable = $true
            }
        } catch {
            Write-Host "  Error al verificar estado, reintentando..." -ForegroundColor Yellow
        }
    }
    
    if ($stable) {
        Write-Success "Servicio estabilizado y funcionando"
        
        if ($PublicIP) {
            try {
                Start-Sleep -Seconds 10
                $response = Invoke-WebRequest -Uri "http://$PublicIP:8080/actuator/health" -TimeoutSec 10 -UseBasicParsing
                if ($response.StatusCode -eq 200) {
                    Write-Success "Health check pasado exitosamente"
                    return $true
                }
            } catch {
                Write-Host "  [!] Health check fallo (la aplicacion puede estar iniciando todavia)" -ForegroundColor Yellow
                return $false
            }
        }
    } else {
        Write-Host "`n  [!] El servicio aun no esta completamente estable" -ForegroundColor Yellow
        return $false
    }
    
    return $stable
}

function Show-DeploymentInfo {
    param(
        [string]$TAG_VERSION,
        [string]$ECR_URL,
        [string]$PublicIP,
        [string]$S3_BUCKET
    )
    
    Write-Host "`n================================================================" -ForegroundColor Green
    Write-Host "DESPLIEGUE COMPLETADO EXITOSAMENTE" -ForegroundColor Green
    Write-Host "================================================================" -ForegroundColor Green
    
    Write-Host "`nINFORMACION DE LA IMAGEN:" -ForegroundColor Cyan
    Write-Host "  Repository: $ECR_URL" -ForegroundColor White
    Write-Host "  Tag Latest: latest" -ForegroundColor White
    Write-Host "  Tag Version: $TAG_VERSION" -ForegroundColor White
    
    Write-Host "`nINFORMACION DE S3:" -ForegroundColor Cyan
    Write-Host "  Bucket: $S3_BUCKET" -ForegroundColor White
    Write-Host "  Region: $AWS_REGION" -ForegroundColor White
    
    if ($PublicIP) {
        Write-Host "`nIP PUBLICA:" -ForegroundColor Cyan
        Write-Host "  $PublicIP" -ForegroundColor Yellow
        
        Write-Host "`nENDPOINTS:" -ForegroundColor Cyan
        Write-Host "  API Base:    http://${PublicIP}:8080" -ForegroundColor Blue
        Write-Host "  Health:      http://${PublicIP}:8080/actuator/health" -ForegroundColor Blue
        Write-Host "  Swagger UI:  http://${PublicIP}:8080/swagger-ui/index.html" -ForegroundColor Blue
        Write-Host "  API Docs:    http://${PublicIP}:8080/v3/api-docs" -ForegroundColor Blue
        
        Write-Host "`nENDPOINTS DE ARCHIVOS:" -ForegroundColor Cyan
        Write-Host "  Subir imagen evento:  POST http://${PublicIP}:8080/api/v1/eventos/{id}/imagen" -ForegroundColor Blue
        Write-Host "  Subir imagen local:   POST http://${PublicIP}:8080/api/v1/locales/{id}/imagen" -ForegroundColor Blue
        Write-Host "  Subir imagen zona:    POST http://${PublicIP}:8080/api/v1/zonas/{id}/imagen" -ForegroundColor Blue
        Write-Host "  Subir multiples:     POST http://${PublicIP}:8080/api/v1/files/{tipo}/{id}/imagenes" -ForegroundColor Blue
        Write-Host "  Eliminar imagen:     DELETE http://${PublicIP}:8080/api/v1/files/imagen?url={url}" -ForegroundColor Blue
    } else {
        Write-Host "`nIP PUBLICA:" -ForegroundColor Yellow
        Write-Host "  No se pudo obtener automaticamente" -ForegroundColor Yellow
        Write-Host "  Ejecuta: .\get-api-url.ps1 para obtenerla" -ForegroundColor Gray
    }
    
    Write-Host "`nMONITOREO:" -ForegroundColor Cyan
    Write-Host "  Logs: aws logs tail $LOG_GROUP --follow --region $AWS_REGION" -ForegroundColor Gray
    Write-Host "  IP:   .\get-api-url.ps1" -ForegroundColor Gray
    
    Write-Host "`nNOTA IMPORTANTE:" -ForegroundColor Yellow
    Write-Host "  La IP publica cambia cada vez que se reinicia la tarea ECS" -ForegroundColor Gray
    Write-Host "  Usa el script get-api-url.ps1 para obtener la IP actual" -ForegroundColor Gray
    Write-Host "  Las imagenes se almacenan en S3: $S3_BUCKET" -ForegroundColor Gray
    Write-Host ""
}

function Start-Deployment {
    $startTime = Get-Date
    
    Write-Host "`n================================================================" -ForegroundColor Cyan
    Write-Host "DESPLIEGUE FASTICKET EN AWS ECS CON S3" -ForegroundColor Cyan
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "Modo:    $(if ($Clean) { 'LIMPIO' } else { 'NORMAL' })" -ForegroundColor White
    Write-Host "Region:  $AWS_REGION" -ForegroundColor White
    Write-Host "Cluster: $CLUSTER_NAME" -ForegroundColor White
    Write-Host "Service: $SERVICE_NAME" -ForegroundColor White
    
    Test-AWSCredentials
    Test-DockerRunning
    
    if ($Clean) {
        Invoke-CleanBuild
    }
    
    $stepNum = if ($Clean) { 5 } else { 2 }
    Write-Step "Obteniendo configuracion de Terraform..." $stepNum
    $ECR_URL = Get-TerraformOutput "ecr_repository_url"
    $S3_BUCKET = Get-TerraformOutput "s3_bucket_name"
    Write-Info "ECR: $ECR_URL"
    Write-Info "S3: $S3_BUCKET"
    
    $stepNum = if ($Clean) { 6 } else { 3 }
    Write-Step "Autenticando en ECR..." $stepNum
    $ECR_REGISTRY = $ECR_URL.Split('/')[0]
    
    # Autenticar con ECR
    $authenticated = $false
    try {
        aws ecr get-login-password --region $AWS_REGION | Out-File -FilePath ecr-pass.txt -Encoding utf8 -NoNewline
        Get-Content ecr-pass.txt | docker login --username AWS --password-stdin $ECR_REGISTRY 2>&1 | Out-Null
        Remove-Item ecr-pass.txt -Force -ErrorAction SilentlyContinue
        if ($LASTEXITCODE -eq 0) {
            $authenticated = $true
            Write-Success "Autenticacion exitosa"
        }
    } catch {
        Remove-Item ecr-pass.txt -Force -ErrorAction SilentlyContinue
    }
    
    if (-not $authenticated) {
        Write-Host "  [!] Autenticacion manual fallo, continuando de todos modos..." -ForegroundColor Yellow
    }
    
    $buildStep = if ($Clean) { 7 } else { 4 }
    $pushStep = if ($Clean) { 8 } else { 5 }
    $TAG_VERSION = Build-DockerImage -UseCache (-not $Clean) -StepNumber $buildStep
    Push-DockerImage -ECR_URL $ECR_URL -TAG_VERSION $TAG_VERSION -StepNumber $pushStep
    
    $updateStep = if ($Clean) { 9 } else { 6 }
    $statusStep = if ($Clean) { 10 } else { 7 }
    Update-ECSService -StepNumber $updateStep
    Get-ServiceStatus -StepNumber $statusStep
    
    $waitStep = if ($Clean) { 11 } else { 8 }
    $getIPStep = if ($Clean) { 12 } else { 9 }
    
    Write-Step "Obteniendo IP publica de la API..." $getIPStep
    $PublicIP = Get-TaskPublicIP
    
    if ($PublicIP) {
        Write-Success "IP publica obtenida: $PublicIP"
        Wait-ServiceStable -PublicIP $PublicIP -StepNumber $waitStep
    } else {
        Write-Host "  [!] No se pudo obtener la IP publica automaticamente" -ForegroundColor Yellow
        Write-Host "  Ejecuta .\get-api-url.ps1 despues del despliegue" -ForegroundColor Gray
    }
    
    $duration = (Get-Date) - $startTime
    Show-DeploymentInfo -TAG_VERSION $TAG_VERSION -ECR_URL $ECR_URL -PublicIP $PublicIP -S3_BUCKET $S3_BUCKET
    
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host "Tiempo total: $($duration.Minutes)m $($duration.Seconds)s" -ForegroundColor Magenta
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host ""
}

try {
    Start-Deployment
} catch {
    Write-Error-Message "Error inesperado durante el despliegue"
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
