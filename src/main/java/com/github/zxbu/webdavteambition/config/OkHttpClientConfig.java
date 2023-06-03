package com.github.zxbu.webdavteambition.config;

import com.github.zxbu.webdavteambition.service.TokenManagerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * OkHttpClient 配置
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class OkHttpClientConfig {

    private final HttpProp httpProp;
    private final TokenManagerService tokenManagerService;

    /**
     * 配置okHttpClient
     */
    @Bean
    public OkHttpClient okHttpClient() {
        // 最大请求数
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(httpProp.getMaxRequest());
        dispatcher.setMaxRequestsPerHost(httpProp.getMaxPerHostRequest());
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // 配置代理, 用于调试
        configProxy(builder);
        // 配置拦截器和授权
        builder.addInterceptor(tokenManagerService.getInterceptor());
        builder.authenticator(tokenManagerService.getAuthenticator());

        return builder.retryOnConnectionFailure(true)
            .connectTimeout(httpProp.getConnectTimeOut(), TimeUnit.SECONDS)
            .writeTimeout(httpProp.getReadTimeOut(),TimeUnit.SECONDS)
            .readTimeout(httpProp.getWriteTimeOut(),TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(httpProp.getMaxConnection(), 5, TimeUnit.MINUTES))
            .dispatcher(dispatcher)
            .build();
    }

    /**
     * 调试使用, 配置fiddler代理
     */
    private void configProxy(OkHttpClient.Builder builder) {
        try (Socket ignored1 = new Socket("127.0.0.1", 18888)) {
            // 如果连接成功，说明该端口是打开的
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 18888));
            builder.proxy((proxy)).sslSocketFactory(sslSocketFactory(), x509TrustManager());
        } catch (Exception ignored) {

        }
    }

    private X509TrustManager x509TrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    private SSLSocketFactory sslSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {x509TrustManager()}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }
}
