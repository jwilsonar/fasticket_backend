# ============================================================================
# Script para ejecutar el seeder en la base de datos de producción
# ============================================================================

Write-Host "`n=== EJECUTAR SEEDER EN PRODUCCIÓN ===" -ForegroundColor Cyan

# Obtener endpoint de RDS
Write-Host "`n[1] Obteniendo endpoint de RDS..." -ForegroundColor Yellow
Push-Location aws\terraform
$endpoint = terraform output -raw rds_endpoint 2>$null
Pop-Location

if (-not $endpoint) {
    Write-Host "[ERROR] No se pudo obtener el endpoint. ¿Está Terraform inicializado?" -ForegroundColor Red
    exit 1
}

$dbHost = $endpoint -replace ':.*', ''
Write-Host "  Endpoint: $dbHost" -ForegroundColor Green

# Verificar conectividad
Write-Host "`n[2] Verificando conectividad..." -ForegroundColor Yellow
$testConnection = Test-NetConnection -ComputerName $dbHost -Port 5432 -WarningAction SilentlyContinue

if (-not $testConnection.TcpTestSucceeded) {
    Write-Host "  [ERROR] No se puede conectar al puerto 5432" -ForegroundColor Red
    Write-Host "  Verifica que tu IP esté en el security group de RDS" -ForegroundColor Yellow
    exit 1
}
Write-Host "  [OK] Puerto 5432 accesible" -ForegroundColor Green

# Buscar PostgreSQL en el sistema
Write-Host "`n[3] Buscando PostgreSQL en el sistema..." -ForegroundColor Yellow

# Rutas comunes de instalación de PostgreSQL
$possiblePaths = @(
    "C:\Program Files\PostgreSQL\*\bin\psql.exe",
    "C:\Program Files (x86)\PostgreSQL\*\bin\psql.exe",
    "$env:ProgramFiles\PostgreSQL\*\bin\psql.exe",
    "$env:LOCALAPPDATA\Programs\PostgreSQL\*\bin\psql.exe"
)

$psqlPath = $null
foreach ($path in $possiblePaths) {
    $found = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $psqlPath = $found.FullName
        break
    }
}

if ($psqlPath) {
    Write-Host "  [OK] PostgreSQL encontrado en: $psqlPath" -ForegroundColor Green
    
    Write-Host "`n[4] Ejecutando seeder en producción..." -ForegroundColor Yellow
    $env:PGPASSWORD = 'DB_fasticket'
    
    & $psqlPath -h $dbHost -U fasticket_admin -d fasticket -f "src/main/resources/db/seeder-postgres.sql"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n  [SUCCESS] Seeder ejecutado exitosamente en producción" -ForegroundColor Green
    } else {
        Write-Host "`n  [ERROR] Hubo un error al ejecutar el seeder" -ForegroundColor Red
        exit 1
    }
    
} else {
    Write-Host "  [INFO] PostgreSQL (psql) no encontrado en el sistema" -ForegroundColor Yellow
    Write-Host "`n  === OPCIONES ALTERNATIVAS ===" -ForegroundColor Cyan
    
    Write-Host "`n  OPCIÓN 1: Usar Docker (Recomendado)" -ForegroundColor White
    Write-Host "  -----------------------------------------" -ForegroundColor Gray
    Write-Host "  docker run --rm -v `${PWD}/src/main/resources/db:/scripts postgres:15 \`" -ForegroundColor White
    Write-Host "    psql -h $dbHost \`" -ForegroundColor White
    Write-Host "    -U fasticket_admin -d fasticket \`" -ForegroundColor White
    Write-Host "    -f /scripts/seeder-postgres.sql" -ForegroundColor White
    Write-Host "  Password: DB_fasticket`n" -ForegroundColor Yellow
    
    Write-Host "  OPCIÓN 2: Usar DBeaver o pgAdmin" -ForegroundColor White
    Write-Host "  -----------------------------------------" -ForegroundColor Gray
    Write-Host "  1. Conecta a la base de datos con:" -ForegroundColor White
    Write-Host "     Host: $dbHost" -ForegroundColor White
    Write-Host "     Puerto: 5432" -ForegroundColor White
    Write-Host "     Database: fasticket" -ForegroundColor White
    Write-Host "     Usuario: fasticket_admin" -ForegroundColor White
    Write-Host "     Password: DB_fasticket" -ForegroundColor White
    Write-Host "  2. Abre y ejecuta: src/main/resources/db/seeder-postgres.sql`n" -ForegroundColor White
    
    Write-Host "  OPCIÓN 3: Instalar PostgreSQL Client" -ForegroundColor White
    Write-Host "  -----------------------------------------" -ForegroundColor Gray
    Write-Host "  winget install PostgreSQL.PostgreSQL`n" -ForegroundColor White
}

Write-Host "`n=================================`n" -ForegroundColor Cyan

