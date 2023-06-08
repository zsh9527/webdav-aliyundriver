package com.github.webdavteambition.service;

import com.github.webdavteambition.config.AliYunClientProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 授权相关功能service
 *
 * @author zsh
 * @version 1.0.0
 * @date 2023/05/29 15:50
 */
@Service
@Slf4j
public class AuthenticateService {

    private final static String AGENT_CONTENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_0_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36";

    @Autowired
    @Lazy
    private AliYunClientProperties aliYunClientProperties;

    /**
     * 自动保存refresh_token
     */
    public Authenticator getAuthenticator() {
        return (route, response) -> {
            if (response.code() == 401 && response.body() != null && response.body().string().contains("AccessToken")) {
                // 失败则刷新token
                aliYunClientProperties.refreshToken(aliYunClientProperties.getRefreshToken());
                return response.request().newBuilder()
                    .removeHeader("authorization")
                    .header("authorization", aliYunClientProperties.getAccessToken())
                    .build();
            }
            return null;
        };
    }

    /**
     * 配置授权部分功能
     */
    public Interceptor getInterceptor() {
        return (chain -> {
            Request request = chain.request();
            Request.Builder builder = request.newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", AGENT_CONTENT)
                .removeHeader("authorization")
                .addHeader("authorization", aliYunClientProperties.getAccessToken());
            if (request.method().equalsIgnoreCase("post")) {
                builder.header("Content-Type", "application/json; charset=UTF-8");
            }
            return chain.proceed(builder.build());
        });
    }
}
