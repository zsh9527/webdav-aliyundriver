package com.github.webdavteambition.task;

import com.github.webdavteambition.client.AliYunDriverClient;
import com.github.webdavteambition.config.AliYunClientProperties;
import com.github.webdavteambition.util.SignUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ali driver 调度任务
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AliSchedulerRunner {

    private final AliYunDriverClient aliYunDriverClient;
    private final AliYunClientProperties aliYunClientProperties;

    /**
     * 每隔5分钟请求一下接口，保证token不过期
     */
    @SneakyThrows
    @Scheduled(initialDelay = 5 * 60 * 1000, fixedDelay = 5 * 60 * 1000)
    public void refreshToken() {
        aliYunDriverClient.getUserInfo();
    }

    /**
     * 每隔5分钟请求一下接口，保证签名不过期
     * renew_session, 签名随机数nonce不为0
     */
    @SneakyThrows
    @Scheduled(initialDelay = 5 * 5 * 1000, fixedDelay = 5 * 60 * 1000)
    public void refreshSignature() {
        var signatureData = SignUtils.signatureByDeviceAndUserAndNonce(aliYunClientProperties.getXDeviceId(),
            aliYunClientProperties.getUserId(), 0);
        // nonce递增
        aliYunDriverClient.createSession(aliYunClientProperties.getXDeviceId(), aliYunClientProperties.getPublicKey(), signatureData);
        aliYunClientProperties.setXSignature(signatureData);
    }

    /**
     * renew_session签名无法通过
     */
    @Deprecated
    @SneakyThrows
    public void refreshSignature2() {
        var signatureData = SignUtils.signatureByDeviceAndUserAndNonce(aliYunClientProperties.getXDeviceId(),
            aliYunClientProperties.getUserId(), aliYunClientProperties.getNonce() + 100);
        // nonce递增
        aliYunDriverClient.renewSession(aliYunClientProperties.getXDeviceId(), signatureData);
        aliYunClientProperties.setNonce(aliYunClientProperties.getNonce() + 1);
        aliYunClientProperties.setXSignature(signatureData);
    }

    /**
     * 每隔10分钟一下保存配置到文件
     */
    @SneakyThrows
    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 10 * 60 * 1000)
    public void saveConfig() {
        aliYunClientProperties.writeConfigToFile();
    }
}
