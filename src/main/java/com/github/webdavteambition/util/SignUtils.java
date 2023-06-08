package com.github.webdavteambition.util;

import com.github.webdavteambition.constant.AliDriverConstants;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 签名工具类 Secp256k1算法签名
 * 参考吾爱破解教程, https://github.com/StickPoint/aliyundrive4j
 * 未找到renew_session可行办法, 通过create_session签名代替
 */
@Slf4j
public class SignUtils {

    /**
     * 签名 -- 相同数据每次签名结果也不一样
     */
    public static String signatureByDeviceAndUserAndNonce(String xDeviceId, String userId, int nonce) {
        // 生成需要签名数据
        String signData = String.format("%s:%s:%s:%d", AliDriverConstants.SECP_APP_ID, xDeviceId, userId, nonce);
        byte[] privateKeyBytes = SignUtils.hexToBytes(xDeviceId);
        byte[] hash = Hash.sha256(signData.getBytes(StandardCharsets.UTF_8));
        return signature(hash, privateKeyBytes);
    }

    /**
     * 签名 -- 相同数据每次签名结果也不一样, renew_session验证通不过
     *
     * @param privateKeyBytes 使用x-device-id作为私钥, 随机生成一个也可以
     */
    @SneakyThrows
    public static String signature(byte[] signData, byte[] privateKeyBytes) {
        ECKeyPair ecKeyPair =  ECKeyPair.create(new BigInteger(1, privateKeyBytes));
        log.info("Raw private key: " + ecKeyPair.getPrivateKey().toString(16));
        log.info("Raw public  key: " + ecKeyPair.getPublicKey().toString(16));
        // 使用 ECKeyPair 对象进行签名
        Sign.SignatureData signatureData = Sign.signMessage(signData, ecKeyPair);
        byte[] signatureBytes = new byte[65];
        System.arraycopy(signatureData.getR(), 0, signatureBytes, 0, 32);
        System.arraycopy(signatureData.getS(), 0, signatureBytes, 32, 32);
        // 最后，为了将签名标记为压缩格式，将字节数组的最后一位设置为0x01, 此处好坑
        signatureBytes[64] = 0x01;
        return bytesToHex(signatureBytes);
    }

    /**
     * 生成公钥 -- 未使用webj3框架
     */
    @SneakyThrows
    public static String generatePublicKey(String privateKeyHex)  {
        byte[] privateKeyBytes = hexToBytes(privateKeyHex);
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        org.bouncycastle.math.ec.ECPoint pointQ = spec.getG().multiply(new BigInteger(1, privateKeyBytes));
        byte[] encoded = pointQ.getEncoded(false);
        // remove prefix;
        return new BigInteger(1, Arrays.copyOfRange(encoded, 1, encoded.length)).toString(16);
    }

    /**
     * 生成公钥
     * 私钥af57abd6dec47f241932edb3475131c4fa6773cb8ab3c0b62bafe948c80e40fe
     * 生成公钥 3e2a3bbd2dfe8675bc40b0b0af6a558421b827b5ad022c8d305eb861b9bfdcc48aea1243df77a6298dfd5cf692a407852b3fd996ea5a13ae213c4380bb7b8d0d
     *
     * @param privateKeyHex 16进制的私钥字符串
     * @return 16进制的公钥字符串
     */
    public static String generatePublicKey2(String privateKeyHex)  {
        byte[] privateKeyBytes = hexToBytes(privateKeyHex);
        ECKeyPair ec =  ECKeyPair.create(new BigInteger(1, privateKeyBytes));
        log.info("Raw private key: " + ec.getPrivateKey().toString(16));
        log.info("Raw public  key: " + ec.getPublicKey().toString(16));
        return ec.getPublicKey().toString(16);
    }

    /**
     * SHA256编码
     */
    public static String getSHA256Encode(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    public static String bytesToHex(byte[] bytes) {
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

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i+1), 16));
        }
        return bytes;
    }


}