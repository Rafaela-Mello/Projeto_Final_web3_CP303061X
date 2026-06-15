# Inicia os três serviços em terminais separados (Windows)

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Iniciando servicos..."
Write-Host "  ms_user  -> http://localhost:8081"
Write-Host "  ms_email -> http://localhost:8082"
Write-Host "  frontend -> http://localhost:3000"
Write-Host ""

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$RootDir\ms_user'; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$RootDir\ms_email'; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$RootDir\frontend'; npm install; npm start"

Write-Host "Tres terminais abertos. Aguarde a inicializacao completa."
