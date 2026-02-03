# Проверка миграций и демо‑seed (PowerShell)

param(
  [string]$ApiBase = $env:API_BASE,
  [string]$DbName = $env:DB_NAME,
  [string]$DbUser = $env:DB_USER,
  [string]$DbPass = $env:DB_PASS,
  [string]$DbPort = $env:DB_PORT,
  [string]$ComposeFile = $env:COMPOSE_FILE
)

if (-not $ApiBase) { $ApiBase = "http://localhost:8080" }
if (-not $DbName) { $DbName = "vw" }
if (-not $DbUser) { $DbUser = "vw" }
if (-not $DbPass) { $DbPass = "vw" }
if (-not $DbPort) { $DbPort = "5432" }
if (-not $ComposeFile) { $ComposeFile = "infra/docker-compose.yml" }

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Write-Host "docker is required."
  exit 1
}

Write-Host "==> Start postgres (docker compose)"
docker compose -f $ComposeFile up -d postgres | Out-Null
$dbContainer = (docker compose -f $ComposeFile ps -q postgres).Trim()
if (-not $dbContainer) {
  Write-Host "Postgres container not found."
  exit 1
}

Write-Host "==> Wait for postgres"
for ($i = 0; $i -lt 30; $i++) {
  docker exec $dbContainer pg_isready -U $DbUser | Out-Null
  if ($LASTEXITCODE -eq 0) { break }
  Start-Sleep -Seconds 1
}

Write-Host "==> Run backend to apply migrations"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:$DbPort/$DbName"
$env:SPRING_DATASOURCE_USERNAME = $DbUser
$env:SPRING_DATASOURCE_PASSWORD = $DbPass
$env:APP_AUTH_COOKIE_SECRET = "dev_cookie_secret_change_me"
$env:APP_QR_HMAC_SECRET = "dev_qr_hmac_secret_change_me"
& ./backend/gradlew -p backend bootRun --args="--spring.main.web-application-type=none --spring.main.banner-mode=off" | Out-Null

Write-Host "==> Verify migrations count"
$migFilesCount = (Get-ChildItem backend/src/main/resources/db/migration | Measure-Object).Count
$applied = docker exec $dbContainer psql -U $DbUser -d $DbName -tAc "select count(*) from flyway_schema_history where success = true;"
$appliedCount = $applied.Trim()
Write-Host "Migrations files: $migFilesCount, applied: $appliedCount"

Write-Host "==> Verify demo seed users"
$seed = docker exec $dbContainer psql -U $DbUser -d $DbName -tAc "select count(*) from staff_users where username in ('admin1','waiter1','kitchen1','superadmin');"
$seedCount = $seed.Trim()
Write-Host "Seed users found: $seedCount"

if ([int]$appliedCount -lt $migFilesCount) {
  Write-Host "Migration count mismatch."
  exit 1
}
if ([int]$seedCount -lt 3) {
  Write-Host "Seed users missing."
  exit 1
}

Write-Host "OK: migrations applied and seed data exists."
