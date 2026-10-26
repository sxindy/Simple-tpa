@echo off
REM ต้องติดตั้ง JDK 21 และ Gradle 8.10 ก่อน (gradle ต้องอยู่ใน PATH)
gradle build
echo.
echo ไฟล์ .jar อยู่ในโฟลเดอร์ build\libs
pause
