package com.github.zxbu.webdavteambition.util;

import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.weaver.Utils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * 签名工具类
 * 此部分代码来自 https://github.com/alist-org/alist, 侵删
 */
@Component
@RequiredArgsConstructor
public class SignUtil {

    private final AliYunDriverClient aliYunDriverClient;
    private static final String secpAppID = "5dde4e1bdf9e4966b387ba58f4b3fdc3";

    @SneakyThrows
    @PostConstruct
    public String initSignature() {
        String userId = aliYunDriverClient.getUserId();
        String deviceID = getSHA256Encode(userId);
        String signData = String.format("%s:%s:%s:%d", secpAppID, deviceID, userId, 0);

        // 计算 SHA-256 哈希值
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(signData.getBytes(StandardCharsets.UTF_8));

        // 使用私钥对哈希值进行签名
        Signature signature = Signature.getInstance("SHA256withECDSA");
        PrivateKey privateKey = getPrivateKeyFromHex(deviceID);
        signature.initSign(privateKey);
        signature.update(hash);
        byte[] signatureBytes = signature.sign();
        // 将签名结果转换为十六进制字符串
        return bytesToHex(signatureBytes);
    }

    public static String getSHA256Encode(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static PrivateKey getPrivateKeyFromHex(String privateKeyHex) throws Exception {
        byte[] privateKeyBytes = hexToBytes(privateKeyHex);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        return privateKey;
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i+1), 16));
        }
        return bytes;
    }

}