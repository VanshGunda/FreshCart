@echo off
setlocal
cd /d "%~dp0"

rem Local FreshCart defaults. Environment variables override these in the current shell.
set "SERVER_PORT=8080"
set "DB_URL=jdbc:mariadb://localhost:3306/freshcart"
set "DB_USERNAME=root"
set "DB_PASSWORD="
set "APP_ADMIN_USERNAME=admin@freshcart.com"
set "APP_ADMIN_PASSWORD=%APP_ADMIN_PASSWORD%"
set "APP_RAZORPAY_KEY_ID=rzp_test_Td60QtsQtZL571"
set "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173"
set "APP_SEED_PRODUCTS=true"
set "APP_REPAIR_INVALID_IMAGE_URLS=true"
set "APP_MAIL_ENABLED=false"
set "APP_OTP_DEV_LOG=true"
set "APP_PAYMENT_AUTO_REFUND_ORPHANED=true"

if "%APP_RAZORPAY_KEY_SECRET%"=="" (
  echo.
  echo [FreshCart] Razorpay TEST secret is not configured.
  echo [FreshCart] Run this once in PowerShell:
  echo.
  echo     .\setup-local.ps1
  echo.
  echo Then run this script again.
  echo.
)

echo [FreshCart] Cleaning stale Maven build output...
call mvnw.cmd clean || exit /b 1
echo [FreshCart] Starting FreshCart locally...
call mvnw.cmd spring-boot:run || exit /b 1
