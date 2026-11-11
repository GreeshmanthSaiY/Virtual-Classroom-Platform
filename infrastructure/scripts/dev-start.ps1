# Virtual Classroom Platform - Local Dev Startup Script for Windows PowerShell
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " Starting Virtual Classroom Platform Local Stack " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Check if Docker is available
$dockerInstalled = Get-Command docker -ErrorAction SilentlyContinue

if ($dockerInstalled) {
    Write-Host "[1/3] Starting PostgreSQL service via Docker Compose..." -ForegroundColor Green
    Set-Location -Path "$PSScriptRoot\..\docker"
    docker compose up -d db
    Set-Location -Path "$PSScriptRoot\..\.."
} else {
    Write-Host "[1/3] Docker not detected. Please ensure local PostgreSQL is running on port 5432." -ForegroundColor Yellow
}

Write-Host "`nTo start the backend in a new terminal:" -ForegroundColor White
Write-Host "  cd backend" -ForegroundColor Gray
Write-Host "  .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`n" -ForegroundColor Gray

Write-Host "To start the frontend in another terminal:" -ForegroundColor White
Write-Host "  cd frontend" -ForegroundColor Gray
Write-Host "  npm.cmd run dev`n" -ForegroundColor Gray
