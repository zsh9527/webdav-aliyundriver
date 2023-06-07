@echo off
:: 挂载webdav到本地批处理脚本

setlocal
set WebDAV_URL=https://127.0.0.1:9027
set DriveLetter=Z:
set Username=admin
set Password=admin

net use %DriveLetter% %WebDAV_URL% /user:%Username% %Password% /persistent:yes

if %errorlevel% equ 0 (
    echo WebDAV挂载成功，盘符为 %DriveLetter%.
) else (
    echo WebDAV挂载失败.
)
endlocal