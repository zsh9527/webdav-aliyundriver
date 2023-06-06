package com.github.zxbu.webdavteambition.util;

import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;

/**
 * 签名工具类
 * 此部分代码来自 https://github.com/alist-org/alist, 侵删
 */
@Component
@RequiredArgsConstructor
public class SignUtil {

    private final AliYunDriverClient aliYunDriverClient;
    private static final String secpAppID = "25dzX3vbYqktVxyX";

    @SneakyThrows
    @PostConstruct
    public void initSignature2() {
        //// 添加Bouncy Castle作为安全提供者
        //Security.addProvider(new BouncyCastleProvider());
        //// 指定椭圆曲线参数
        //ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        //// 创建ECDSA密钥对生成器
        //KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        //keyPairGenerator.initialize(ecSpec);
        //// 生成密钥对
        //KeyPair keyPair = keyPairGenerator.generateKeyPair();
        //// 获取私钥
        //// Get the public key
        //ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        //byte[] publicKeyBytes = publicKey.getQ().getEncoded(false); // uncompressed format
        //String publicKeyHex = "04" + Hex.toHexString(publicKeyBytes);
        //
        //System.out.println("Private Key: " + privateExponent.toString(16));
        //System.out.println("Public Key: " + publicKeyHex);
        //// 将私钥打印出来
        //System.out.println("Private Key: " + privateKey.getS());
    }

    @SneakyThrows
    @PostConstruct
    public String initSignature() {
        // 添加Bouncy Castle作为安全提供者
        Security.addProvider(new BouncyCastleProvider());
        String userId = aliYunDriverClient.getUserId();
        String deviceID = getSHA256Encode(userId);
        // 计算 SHA-256 哈希值
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String signData = String.format("%s:%s:%s:%d", secpAppID, deviceID, userId, 0);
        byte[] hash = digest.digest(signData.getBytes(StandardCharsets.UTF_8));
        PrivateKey privateKey = getPrivateKeyFromHex(deviceID);
        //ECPrivateKey privateKey = new BCECPrivateKey("EC", new ECPrivateKeyParameters(state.getPrivateKey(), domain));
        // 使用私钥对哈希值进行签名
        Signature signature = Signature.getInstance("SHA256withECDSA");
        //PrivateKey privateKey = getPrivateKeyFromHex(deviceID);
        signature.initSign(privateKey);
        signature.update(hash);
        byte[] signatureBytes = signature.sign();
        //// 将签名结果转换为十六进制字符串
        String t = bytesToHex(signatureBytes);
        var t1 = "";
        return t;
    }

    /**
     * SHA256编码
     */
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
        try {
            // 添加Bouncy Castle作为安全提供者
            Security.addProvider(new BouncyCastleProvider());
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECParameterSpec ecParameterSpec = new ECParameterSpec(spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
            ECPrivateKeySpec privateKeyParameters = new ECPrivateKeySpec(new BigInteger(1, privateKeyBytes), ecParameterSpec);
            // 创建私钥对象
            ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
            keyPairGenerator.init(privateKeyParameters);
            AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();


            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeyParameters);
            keyFactory.generatePublic(privateKeyParameters);
            return privateKey;
            //ECKeyGenerationParameters keyGenerationParameters = new ECKeyGenerationParameters(privateKeyParameters.getParameters(), null);
            //ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
            //keyPairGenerator.init(keyGenerationParameters);
            //AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();
            //return (ECPrivateKeyParameters) keyPair.getPrivate();
            //X9ECParameters curveParams = ECNamedCurveTable.getParameterSpec("secp256k1");
            //ECPrivateKeyParameters privateKeyParameters = ECPrivateKeyParameters(new BigInteger(1, privateKeyBytes),
            //    curveParams);
            //ECPublicKeyParameters publicKeyParameters = privateKeyParameters;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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