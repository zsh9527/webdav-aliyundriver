package com.github.zxbu.webdavteambition.service;

import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
import com.github.zxbu.webdavteambition.config.AliYunDriveProperties;
import com.github.zxbu.webdavteambition.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * TokenManagerService
 *
 * @author zsh
 * @version 1.0.0
 * @date 2023/05/29 15:50
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenManagerService {

    private final AliYunDriveProperties aliYunDriveProperties;
    private final static String AGENT_CONTENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_0_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36";

    @Autowired
    @Lazy
    private AliYunDriverClient aliYunDriverClient;

    private String accessToken = "";

    /**
     * 自动保存refresh_token
     */
    public Authenticator getAuthenticator() {
        return (route, response) -> {
            if (response.code() == 401 && response.body() != null && response.body().string().contains("AccessToken")) {
                String refreshTokenResult;
                Map<String, String> params = new HashMap<>();
                params.put("refresh_token", readRefreshToken());
                params.put("grant_type", "refresh_token");
                try {
                    refreshTokenResult = aliYunDriverClient.post("https://auth.aliyundrive.com/v2/account/token", params);
                } catch (Exception e) {
                    // 如果置换token失败，先清空原token文件，再尝试一次
                    deleteRefreshTokenFile();
                    refreshTokenResult = aliYunDriverClient.post("https://auth.aliyundrive.com/v2/account/token", params);
                }
                String accessToken = (String) JsonUtil.getJsonNodeValue(refreshTokenResult, "access_token");
                String refreshToken = (String) JsonUtil.getJsonNodeValue(refreshTokenResult, "refresh_token");
                Assert.hasLength(accessToken, "获取accessToken失败");
                Assert.hasLength(refreshToken, "获取refreshToken失败");
                this.accessToken = accessToken;
                writeRefreshToken(refreshToken);
                return response.request().newBuilder()
                    .removeHeader("authorization")
                    .header("authorization", accessToken)
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
            request = request.newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", AGENT_CONTENT)
                .removeHeader("authorization")
                .addHeader("authorization", accessToken)
                .build();
            return chain.proceed(request);
        });
    }

    /**
     * 从配置文件workDir目录下文件读取refresh-token
     *
     * @return refresh-token
     */
    private String readRefreshToken() {
        String refreshTokenPath = aliYunDriveProperties.getWorkDir() + "refresh-token";
        Path path = Paths.get(refreshTokenPath);

        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length != 0) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("读取refreshToken文件 {} 失败: ", refreshTokenPath, e);
        }
        writeRefreshToken(aliYunDriveProperties.getRefreshToken());
        return aliYunDriveProperties.getRefreshToken();
    }

    private void deleteRefreshTokenFile() {
        String refreshTokenPath = aliYunDriveProperties.getWorkDir() + "refresh-token";
        Path path = Paths.get(refreshTokenPath);
        try {
            Files.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeRefreshToken(String newRefreshToken) {
        String refreshTokenPath = aliYunDriveProperties.getWorkDir() + "refresh-token";
        try {
            Files.writeString(Paths.get(refreshTokenPath), newRefreshToken);
        } catch (IOException e) {
            log.warn("写入refreshToken文件 {} 失败: ", refreshTokenPath, e);
        }
        aliYunDriveProperties.setRefreshToken(newRefreshToken);
    }

}
