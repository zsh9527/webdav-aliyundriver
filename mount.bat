@echo off
@chcp 65001
:: 挂载WebDAV到本地盘符
@set WEBDAV_SERVER=http://127.0.0.1:9027
set DRIVE_LETTER=Z:
set USERNAME=admin
set PASSWORD=admin

@echo on
REM 取消挂载命令为 net use Z: /delete
@echo off

net use %DRIVE_LETTER% %WEBDAV_SERVER% /user:%USERNAME% %PASSWORD% /persistent:yes >nul 2>&1


if %ERRORLEVEL% equ 0 (
    echo 挂载成功, 盘符为： %DRIVE_LETTER%
) else (
    echo 挂载失败, 请阅读MountHelp.md
)
