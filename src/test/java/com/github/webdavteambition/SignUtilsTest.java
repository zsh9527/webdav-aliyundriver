package com.github.webdavteambition;

import com.github.webdavteambition.util.SignUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 签名工具类测试
 */
public class SignUtilsTest {

    /**
     * 生成公钥
     */
    @Test
    void testGeneratePublicKey() {
        var privateKeyHex = "af57abd6dec47f241932edb3475131c4fa6773cb8ab3c0b62bafe948c80e40fe";
        var publicKeyHex = SignUtils.generatePublicKey(privateKeyHex);
        Assertions.assertEquals("3e2a3bbd2dfe8675bc40b0b0af6a558421b827b5ad022c8d305eb861b9bfdcc48aea1243df77a6298dfd5cf692a407852b3fd996ea5a13ae213c4380bb7b8d0d", publicKeyHex);
    }

    /**
     * 生成公钥
     */
    @Test
    void testGeneratePublicKeyTwice() {
        var privateKeyHex = "c86e551da24af9e295ac62bea87d3f5492193ea1bf1e0e4aa1dba935c8dced38";
        var publicKeyHex = SignUtils.generatePublicKey(privateKeyHex);
        Assertions.assertEquals("66ace7f6067be7fed847e768981da5813327b340bc2232bef91e6bbb323c0c934248347eb9c2c74f3450ab26a8bb469513bb52d1e7fd8346a35afff6619104d6", publicKeyHex);
    }

    /**
     * 生成公钥
     */
    @Test
    void testGeneratePublicKey2() {
        var privateKeyHex = "af57abd6dec47f241932edb3475131c4fa6773cb8ab3c0b62bafe948c80e40fe";
        var publicKeyHex = SignUtils.generatePublicKey2(privateKeyHex);
        Assertions.assertEquals("3e2a3bbd2dfe8675bc40b0b0af6a558421b827b5ad022c8d305eb861b9bfdcc48aea1243df77a6298dfd5cf692a407852b3fd996ea5a13ae213c4380bb7b8d0d", publicKeyHex);
    }

    ///**
    // * 签名
    // */
    //@SneakyThrows
    //@Test
    //void testSignature() {
    //    String signDate = "123";
    //    // 使用私钥对哈希值进行签名
    //    String t = SignUtils.loadPrivateKey("365cb54a2e402d48ddcbffd1e19df38b84e740ea6b0535f32dd49cc23435f61e");
    //    //// 将签名结果转换为十六进制字符串
    //    Assertions.assertEquals("5b142f5e1c29dca3ae0d2705d4ac61c47b503560147786a83d2812dff5dcd01f7411a06f866ec1aaa61167a6bde1d77e82797b383950cfad424a4d77ccf486ad01", t);
    //}
}
