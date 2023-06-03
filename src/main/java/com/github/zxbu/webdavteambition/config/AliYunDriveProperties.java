package com.github.zxbu.webdavteambition.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aliyundrive")
public class AliYunDriveProperties {

    private String url = "https://api.aliyundrive.com";
    private String workDir = "/etc/aliyun-driver/";
    private String refreshToken = "";
    private Auth auth;

    public static class Auth {
        private Boolean enable = true;
        private String userName;
        private String password;

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            this.enable = enable;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
