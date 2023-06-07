package com.github.webdavteambition.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.webdavteambition.constant.AliDriverConstants;
import com.github.webdavteambition.model.result.TokenResp;
import com.github.webdavteambition.model.result.UserInfoResp;
import com.github.webdavteambition.util.SignUtils;
import com.github.webdavteambition.client.AliYunDriverClient;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Hash;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * 阿里云client相关属性
 */
@Getter
@Setter
@Component
@Slf4j
public class AliYunClientProperties implements Serializable {

    @JsonIgnore
    private final AliYunDriverClient aliYunDriverClient;

    @JsonIgnore
    private final AliYunDriveProperties aliYunDriveProperties;

    @JsonIgnore
    private final ObjectMapper objectMapper;

    public AliYunClientProperties(AliYunDriverClient aliYunDriverClient, AliYunDriveProperties aliYunDriveProperties, ObjectMapper objectMapper) {
        this.aliYunDriverClient = aliYunDriverClient;
        this.aliYunDriveProperties = aliYunDriveProperties;
        this.objectMapper = objectMapper;
    }

    @JsonIgnore
    private String filename;

    @PostConstruct
    void init() {
        initFilename();
        initConfigFromFile();
        initAccessToken();
        initUserConfig();
        initSignConfig();
        writeConfigToFile();
    }

    void initFilename() {
        this.filename = aliYunDriveProperties.getWorkDir() + "config.json";
    }

    /**
     * 从配置文件workDir目录下文件读取配置
     */
    void initConfigFromFile() {
        File file = new File(filename);
        if (file.exists()) {
            try {
                AliYunClientProperties fileConfig = objectMapper.readValue(file, AliYunClientProperties.class);
                this.driveId = fileConfig.driveId;
                this.userId = fileConfig.userId;
                this.xDeviceId = fileConfig.xDeviceId;
                this.xSignature = fileConfig.xSignature;
            } catch (IOException e) {
                log.warn("读取配置文件失败");
            }
        }
    }

    /**
     * 初始化accessToken
     */
    @SneakyThrows
    void initAccessToken() {
        if (!StringUtils.hasLength(this.accessToken)) {
            refreshToken(aliYunDriveProperties.getRefreshToken());
        }
    }

    /**
     * 初始化用户信息
     */
    @SneakyThrows
    @PostConstruct
    void initUserConfig() {
          UserInfoResp userInfoResp = aliYunDriverClient.getUserInfo();
          this.driveId = userInfoResp.getDefault_drive_id();
          this.userId = userInfoResp.getUser_id();
    }

    /**
     * 初始化签名信息
     */
    @SneakyThrows
    void initSignConfig() {
        if (!StringUtils.hasLength(this.xDeviceId) || !StringUtils.hasLength(this.xSignature)) {
            // x-device-id 可以使用随机数, 没有限制, 作为私钥使用
            var xDeviceId = SignUtils.getSHA256Encode(this.userId);
            var publicKey = SignUtils.generatePublicKey(xDeviceId);
            // 生成需要签名数据
            String signData = String.format("%s:%s:%s:%d", AliDriverConstants.SECP_APP_ID, xDeviceId, userId, 0);
            byte[] privateKeyBytes = SignUtils.hexToBytes(xDeviceId);
            byte[] hash = Hash.sha256(signData.getBytes(StandardCharsets.UTF_8));
            // 使用 ECKeyPair 对象进行签名
            var signatureData = SignUtils.signature(hash, privateKeyBytes);
            if (aliYunDriverClient.createSession(xDeviceId, publicKey, signatureData)) {
                this.setXDeviceId(xDeviceId);
                this.setXDeviceId(signData);
            }
        }
    }

    /**
     * 刷新token
     */
    @SneakyThrows
    public void refreshToken(String refreshToken) {
        TokenResp tokenResp = aliYunDriverClient.getToken(refreshToken);
        Assert.hasLength(tokenResp.getAccess_token(), "获取accessToken失败");
        Assert.hasLength(tokenResp.getRefresh_token(), "获取refreshToken失败");
        this.setAccessToken(tokenResp.getAccess_token());
        this.setRefreshToken(tokenResp.getRefresh_token());
    }

    @JsonIgnore
    private String driveId;

    @JsonIgnore
    private String userId;

    private String xDeviceId;

    private String xSignature;

    private String accessToken = "";

    private String refreshToken;

    /**
     * 保存配置到文件
     */
    @SneakyThrows
    public void writeConfigToFile() {
        File file = new File(filename);
        File directory = file.getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        objectMapper.writeValue(file, this);
    }
}
