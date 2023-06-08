> 由于windows默认不支持http协议的挂载, 所以需要修改注册表  
1. 修改注册表
注册表(regedit) - HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\WebClient\Parameters
把BasicAuthLevel 值改成2，即同时支持http和https，默认只支持https，然后重启服务：

2. 重启服务 
服务(services.msc)- WebClient设为停止再启动

3. 执行mount.bat

4. 取消挂载
`net use Z: /delete`