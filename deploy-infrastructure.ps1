# Script para desplegar infraestructura AWS desde cero
param(
    [switch]$Destroy = $false
)

$ErrorActionPreference = "Stop"
$AWS_REGION = "us-east-1"

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
        Write-Info "Account: $($identity | ConvertFrom-Json | Select-Object -ExpandProperty Account)"
        return $true
    } catch {
        Exit-WithError "Credenciales de AWS invalidas o expiradas. Ejecuta: aws configure"
    }
}

function Test-Terraform {
    Write-Step "Verificando Terraform..." 2
    try {
        $version = terraform --version 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Terraform no encontrado"
        }
        Write-Success "Terraform disponible"
        Write-Info "Version: $($version[0])"
    } catch {
        Exit-WithError "Terraform no esta instalado o no esta en el PATH"
    }
}

function Setup-TerraformVars {
    Write-Step "Configurando variables de Terraform..." 3
    
    $tfvarsPath = "aws\terraform\terraform.tfvars"
    $tfvarsExamplePath = "aws\terraform\terraform.tfvars.example"
    
    if (-not (Test-Path $tfvarsPath)) {
        if (Test-Path $tfvarsExamplePath) {
            Copy-Item $tfvarsExamplePath $tfvarsPath
            Write-Success "Archivo terraform.tfvars creado desde ejemplo"
            Write-Host "`nIMPORTANTE: Edita aws\terraform\terraform.tfvars con tus valores:" -ForegroundColor Yellow
            Write-Host "  - db_password: Cambia la contraseña de la base de datos" -ForegroundColor Yellow
            Write-Host "  - frontend_url: URL de tu frontend" -ForegroundColor Yellow
            Write-Host "`nPresiona Enter cuando hayas editado el archivo..." -ForegroundColor Cyan
            Read-Host
        } else {
            Exit-WithError "No se encontro terraform.tfvars.example"
        }
    } else {
        Write-Success "terraform.tfvars ya existe"
    }
}

function Create-S3Backend {
    Write-Step "Creando bucket S3 para estado de Terraform..." 4
    
    $bucketName = "fasticket-terraform-state-$((Get-Date).ToFileTime())"
    
    try {
        aws s3 mb "s3://$bucketName" --region $AWS_REGION 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Bucket S3 creado: $bucketName"
            
            # Actualizar main.tf con el bucket
            $mainTfPath = "aws\terraform\main.tf"
            $content = Get-Content $mainTfPath -Raw
            $newContent = $content -replace 'bucket = "fasticket-terraform-state-XXXXXXXXXX"', "bucket = `"$bucketName`""
            Set-Content $mainTfPath $newContent -NoNewline
            
            Write-Success "main.tf actualizado con bucket: $bucketName"
            return $bucketName
        } else {
            Exit-WithError "Error al crear bucket S3"
        }
    } catch {
        Exit-WithError "Error al crear bucket S3: $($_.Exception.Message)"
    }
}

function Initialize-Terraform {
    Write-Step "Inicializando Terraform..." 5
    
    Push-Location aws\terraform
    try {
        terraform init -reconfigure 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Terraform inicializado"
        } else {
            Pop-Location
            Exit-WithError "Error al inicializar Terraform"
        }
    } catch {
        Pop-Location
        Exit-WithError "Error al inicializar Terraform: $($_.Exception.Message)"
    }
    Pop-Location
}

function Plan-Terraform {
    Write-Step "Planificando recursos..." 6
    
    Push-Location aws\terraform
    try {
        Write-Info "Ejecutando terraform plan..."
        terraform plan -out=tfplan 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Plan de Terraform generado"
        } else {
            Pop-Location
            Exit-WithError "Error en terraform plan"
        }
    } catch {
        Pop-Location
        Exit-WithError "Error en terraform plan: $($_.Exception.Message)"
    }
    Pop-Location
}

function Apply-Terraform {
    Write-Step "Aplicando infraestructura..." 7
    Write-Host "  Esto puede tomar 15-20 minutos..." -ForegroundColor Gray
    
    Push-Location aws\terraform
    try {
        Write-Info "Ejecutando terraform apply..."
        terraform apply -auto-approve tfplan 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Infraestructura desplegada exitosamente"
        } else {
            Pop-Location
            Exit-WithError "Error en terraform apply"
        }
    } catch {
        Pop-Location
        Exit-WithError "Error en terraform apply: $($_.Exception.Message)"
    }
    Pop-Location
}

function Save-Outputs {
    Write-Step "Guardando outputs..." 8
    
    Push-Location aws\terraform
    try {
        terraform output > ..\..\outputs.txt
        Write-Success "Outputs guardados en outputs.txt"
        
        # Mostrar outputs importantes
        Write-Host "`nOUTPUTS IMPORTANTES:" -ForegroundColor Cyan
        $ecrUrl = terraform output -raw ecr_repository_url
        $s3Bucket = terraform output -raw s3_bucket_name
        $rdsEndpoint = terraform output -raw rds_endpoint
        
        Write-Host "  ECR Repository: $ecrUrl" -ForegroundColor White
        Write-Host "  S3 Bucket: $s3Bucket" -ForegroundColor White
        Write-Host "  RDS Endpoint: $rdsEndpoint" -ForegroundColor White
        
    } catch {
        Pop-Location
        Write-Host "  [!] Error al guardar outputs" -ForegroundColor Yellow
    }
    Pop-Location
}

function Destroy-Infrastructure {
    Write-Step "DESTRUYENDO INFRAESTRUCTURA..." 1
    Write-Host "  ⚠️  ESTO ELIMINARA TODOS LOS RECURSOS DE AWS" -ForegroundColor Red
    Write-Host "  ⚠️  ESTA ACCION NO SE PUEDE DESHACER" -ForegroundColor Red
    Write-Host ""
    $confirm = Read-Host "Escribe 'DESTROY' para confirmar"
    
    if ($confirm -ne "DESTROY") {
        Write-Host "Operacion cancelada" -ForegroundColor Yellow
        exit 0
    }
    
    Push-Location aws\terraform
    try {
        Write-Info "Ejecutando terraform destroy..."
        terraform destroy -auto-approve 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Infraestructura destruida exitosamente"
        } else {
            Pop-Location
            Exit-WithError "Error en terraform destroy"
        }
    } catch {
        Pop-Location
        Exit-WithError "Error en terraform destroy: $($_.Exception.Message)"
    }
    Pop-Location
}

function Show-NextSteps {
    Write-Host "`n================================================================" -ForegroundColor Green
    Write-Host "INFRAESTRUCTURA DESPLEGADA EXITOSAMENTE" -ForegroundColor Green
    Write-Host "================================================================" -ForegroundColor Green
    
    Write-Host "`nPROXIMOS PASOS:" -ForegroundColor Cyan
    Write-Host "  1. Ejecuta: .\deploy-with-s3.ps1" -ForegroundColor White
    Write-Host "     Para desplegar la aplicacion Spring Boot" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  2. O ejecuta: .\deploy.ps1" -ForegroundColor White
    Write-Host "     Para despliegue sin S3 (version anterior)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  3. Verifica los outputs en: outputs.txt" -ForegroundColor White
    Write-Host ""
    Write-Host "  4. Para destruir todo: .\deploy-infrastructure.ps1 -Destroy" -ForegroundColor Red
    Write-Host ""
}

function Start-InfrastructureDeployment {
    $startTime = Get-Date
    
    Write-Host "`n================================================================" -ForegroundColor Cyan
    Write-Host "DESPLIEGUE DE INFRAESTRUCTURA AWS FASTICKET" -ForegroundColor Cyan
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "Region: $AWS_REGION" -ForegroundColor White
    Write-Host "Modo: $(if ($Destroy) { 'DESTRUIR' } else { 'CREAR' })" -ForegroundColor White
    
    if ($Destroy) {
        Destroy-Infrastructure
        return
    }
    
    Test-AWSCredentials
    Test-Terraform
    Setup-TerraformVars
    Create-S3Backend
    Initialize-Terraform
    Plan-Terraform
    Apply-Terraform
    Save-Outputs
    
    $duration = (Get-Date) - $startTime
    Show-NextSteps
    
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host "Tiempo total: $($duration.Minutes)m $($duration.Seconds)s" -ForegroundColor Magenta
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host ""
}

try {
    Start-InfrastructureDeployment
} catch {
    Write-Error-Message "Error inesperado durante el despliegue de infraestructura"
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
