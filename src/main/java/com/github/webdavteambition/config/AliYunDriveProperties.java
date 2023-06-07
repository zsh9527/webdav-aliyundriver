package com.github.webdavteambition.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aliyundrive")
public class AliYunDriveProperties {

    // 配置文件存放目录, 主要用作备份access_token, x-device-id, x-signature等数据
    private String workDir;
    // 仅用于initAccessToken方法
    private String refreshToken;
    // webdav 授权管理
    private WebDavAuth auth;

    @Data
    public static class WebDavAuth {
        private Boolean enable = true;
        private String userName;
        private String password;
    }
}
