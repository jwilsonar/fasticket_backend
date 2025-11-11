# Script de Despliegue Rapido - FastTicket
# Uso: .\deploy-quick.ps1
# Para cambios menores sin rebuild completo

$ErrorActionPreference = "Stop"

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "DESPLIEGUE RAPIDO - FASTICKET" -ForegroundColor Cyan
Write-Host "================================================================`n" -ForegroundColor Cyan

# 0. Verificar credenciales AWS
Write-Host "[0/5] Verificando credenciales AWS..." -ForegroundColor Yellow

$awsCommand = Get-Command aws -ErrorAction SilentlyContinue
if (-not $awsCommand) {
    $awsCommand = Get-Command aws.exe -ErrorAction SilentlyContinue
}

if (-not $awsCommand) {
    Write-Host "`n[ERROR] AWS CLI no está instalado o no está en el PATH" -ForegroundColor Red
    Write-Host "  Descarga e instala AWS CLI v2 desde:" -ForegroundColor Yellow
    Write-Host "    https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html" -ForegroundColor White
    Write-Host "  Luego cierra y vuelve a abrir PowerShell, o agrega la ruta de instalación al PATH." -ForegroundColor White
    exit 1
}

$awsCliPath = $awsCommand.Source
$awsIdentity = & $awsCliPath sts get-caller-identity 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERROR] Credenciales AWS no configuradas o inválidas" -ForegroundColor Red
    Write-Host "`nPara AWS Academy Lab:" -ForegroundColor Yellow
    Write-Host "  1. Ve a tu AWS Academy Lab" -ForegroundColor White
    Write-Host "  2. Haz clic en 'AWS Details'" -ForegroundColor White
    Write-Host "  3. Haz clic en 'Show' en AWS CLI credentials" -ForegroundColor White
    Write-Host "  4. Copia las credenciales y pégalas en: ~/.aws/credentials" -ForegroundColor White
    Write-Host "     (o en Windows: C:\Users\<tu-usuario>\.aws\credentials)" -ForegroundColor White
    Write-Host "`nO configura las variables de entorno:" -ForegroundColor Yellow
    Write-Host "  `$env:AWS_ACCESS_KEY_ID='tu-access-key'" -ForegroundColor White
    Write-Host "  `$env:AWS_SECRET_ACCESS_KEY='tu-secret-key'" -ForegroundColor White
    Write-Host "  `$env:AWS_SESSION_TOKEN='tu-session-token'`n" -ForegroundColor White
    exit 1
}
Write-Host "  [OK] Credenciales válidas" -ForegroundColor Green

# 1. Build de imagen
Write-Host "`n[1/5] Construyendo imagen Docker..." -ForegroundColor Yellow
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
function Get-EcrPassword {
    return (aws ecr get-login-password --region us-east-1 2>&1)
}

$awsPasswordOutput = Get-EcrPassword
$awsPasswordExitCode = $LASTEXITCODE

if ($awsPasswordExitCode -ne 0 -or -not $awsPasswordOutput) {
    Write-Host "  [ERROR] No se pudo obtener token de ECR" -ForegroundColor Red
    Write-Host "  Verifica que tus credenciales tengan permisos para ECR (GetAuthorizationToken)" -ForegroundColor Yellow
    exit 1
}

$awsPassword = ($awsPasswordOutput | Out-String).Trim()

function Invoke-DockerLogin {
    param (
        [string]$Registry,
        [string]$Password
    )

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $result = $Password | docker login --username AWS --password-stdin $Registry 2>&1
        $exit = $LASTEXITCODE

        if ($exit -ne 0 -and $Password) {
            $escapedPassword = $Password.Replace("`n","").Replace("`r","")
            $result = docker login --username AWS --password $escapedPassword $Registry 2>&1
            $exit = $LASTEXITCODE
        }
    } finally {
        $ErrorActionPreference = $previousPreference
    }

    return @{ Output = $result; ExitCode = $exit }
}

$loginResult = Invoke-DockerLogin -Registry $ECR_REGISTRY -Password $awsPassword
$dockerLoginMessages = $loginResult.Output
$dockerLoginExit = $loginResult.ExitCode

if ($dockerLoginExit -ne 0) {
    Write-Host "  [!] Reintentando autenticacion..." -ForegroundColor Yellow
    docker logout $ECR_REGISTRY 2>&1 | Out-Null

    $awsPasswordOutput = Get-EcrPassword
    $awsPasswordExitCode = $LASTEXITCODE

    if ($awsPasswordExitCode -ne 0 -or -not $awsPasswordOutput) {
        Write-Host "  [ERROR] No se pudo obtener token de ECR en el reintento" -ForegroundColor Red
        exit 1
    }

    $awsPassword = ($awsPasswordOutput | Out-String).Trim()
    $loginResult = Invoke-DockerLogin -Registry $ECR_REGISTRY -Password $awsPassword
    $dockerLoginMessages = $loginResult.Output
    $dockerLoginExit = $loginResult.ExitCode
}

if ($dockerLoginExit -eq 0) {
    Write-Host "  [OK] Autenticado con ECR" -ForegroundColor Green
} else {
    Write-Host "  [ERROR] Falló la autenticacion con Docker (HTTP 400)" -ForegroundColor Red
    if ($dockerLoginMessages) {
        Write-Host "    Detalle: $dockerLoginMessages" -ForegroundColor Yellow
    }
    Write-Host "    - Revisa que el AWS Session Token no esté expirado" -ForegroundColor White
    Write-Host "    - Ejecuta .\setup-aws-credentials.ps1 y vuelve a intentar" -ForegroundColor White
    Write-Host "    - Comprueba que la cuenta tenga acceso al repositorio $ECR_REGISTRY" -ForegroundColor White
    exit 1
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

