@echo off
:: ����webdav������������ű�

setlocal
set WebDAV_URL=https://127.0.0.1:9027
set DriveLetter=Z:
set Username=admin
set Password=admin

net use %DriveLetter% %WebDAV_URL% /user:%Username% %Password% /persistent:yes

if %errorlevel% equ 0 (
    echo WebDAV���سɹ����̷�Ϊ %DriveLetter%.
) else (
    echo WebDAV����ʧ��.
)
endlocal