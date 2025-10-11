# Script para obtener información de conexión a RDS

Write-Host "`n=== INFORMACIÓN DE CONEXIÓN RDS ===" -ForegroundColor Cyan

Write-Host "`n[1] Obteniendo datos del endpoint..." -ForegroundColor Yellow
Push-Location aws\terraform
$endpoint = terraform output -raw rds_endpoint 2>$null
Pop-Location

if (-not $endpoint) {
    Write-Host "[ERROR] No se pudo obtener el endpoint. ¿Está Terraform inicializado?" -ForegroundColor Red
    exit 1
}

$dbHost = $endpoint -replace ':.*', ''

Write-Host "`n=== DATOS PARA DBEAVER ===" -ForegroundColor Green
Write-Host "Host:     $dbHost" -ForegroundColor White
Write-Host "Puerto:   5432" -ForegroundColor White
Write-Host "Database: fasticket" -ForegroundColor White
Write-Host "Usuario:  fasticket_admin" -ForegroundColor White
Write-Host "Password: DB_fasticket" -ForegroundColor White
Write-Host "===========================" -ForegroundColor Green

Write-Host "`n[2] Verificando conectividad..." -ForegroundColor Yellow
$testConnection = Test-NetConnection -ComputerName $dbHost -Port 5432 -WarningAction SilentlyContinue

if ($testConnection.TcpTestSucceeded) {
    Write-Host "  [OK] Puerto 5432 accesible desde tu IP" -ForegroundColor Green
} else {
    Write-Host "  [X] Puerto 5432 NO accesible" -ForegroundColor Red
    Write-Host "`n  Solución: Agrega tu IP al Security Group de RDS" -ForegroundColor Yellow
    
    Write-Host "`n[3] Obteniendo tu IP pública..." -ForegroundColor Yellow
    try {
        $myIp = (Invoke-WebRequest -Uri "https://api.ipify.org" -TimeoutSec 5).Content
        Write-Host "  Tu IP pública: $myIp" -ForegroundColor Cyan
        
        Write-Host "`n  Ejecuta este comando para permitir tu IP:" -ForegroundColor Yellow
        Write-Host "  aws ec2 authorize-security-group-ingress --group-id <SG-ID> --protocol tcp --port 5432 --cidr ${myIp}/32 --region us-east-1" -ForegroundColor Gray
    } catch {
        Write-Host "  No se pudo obtener tu IP pública" -ForegroundColor Yellow
    }
}

Write-Host "`n[4] Comando psql (si tienes PostgreSQL client instalado):" -ForegroundColor Yellow
$psqlCommand = "psql -h $dbHost -U fasticket_admin -d fasticket -p 5432"
Write-Host "  $psqlCommand" -ForegroundColor Cyan

Write-Host "`n[5] Variables de entorno (para scripts):" -ForegroundColor Yellow
Write-Host "  DB_HOST: $dbHost" -ForegroundColor Gray
Write-Host "  DB_PORT: 5432" -ForegroundColor Gray
Write-Host "  DB_NAME: fasticket" -ForegroundColor Gray
Write-Host "  DB_USER: fasticket_admin" -ForegroundColor Gray
Write-Host "  DB_PASS: DB_fasticket" -ForegroundColor Gray

Write-Host "`n[6] URL de conexión JDBC:" -ForegroundColor Yellow
$jdbcUrl = "jdbc:postgresql://${dbHost}:5432/fasticket"
Write-Host "  $jdbcUrl" -ForegroundColor Cyan

# Copiar host al portapapeles
try {
    Set-Clipboard -Value $dbHost
    Write-Host "`n[OK] Host copiado al portapapeles!" -ForegroundColor Green
} catch {
    # Silenciar error si no hay clipboard disponible
}

Write-Host "`n[INFO] Consulta DBEAVER-RDS-CONNECTION.md para instrucciones detalladas" -ForegroundColor Cyan
Write-Host ""

