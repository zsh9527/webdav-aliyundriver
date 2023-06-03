package com.github.zxbu.webdavteambition.client;

import com.github.zxbu.webdavteambition.config.AliYunDriveProperties;
import com.github.zxbu.webdavteambition.util.JsonUtil;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.webdav.exceptions.WebdavException;
import okhttp3.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * okhttpClient请求数据统一封装和解析 + 几个特定请求
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Lazy
@Retry(name = "retry-backend")
public class AliYunDriverClient {
    private final OkHttpClient okHttpClient;
    private final AliYunDriveProperties aliYunDriveProperties;
    private String deviceId;
    private String userId;

    /**
     * 获取device_id, 用于后续请求
     */
    @PostConstruct
    public void init() throws IOException {
        String personalJson = post("/v2/user/get", Collections.emptyMap());
        this.deviceId = (String) JsonUtil.getJsonNodeValue(personalJson, "default_drive_id");
        this.userId = (String) JsonUtil.getJsonNodeValue(personalJson, "user_id");
    }

    public String getDriveId() {
        return this.deviceId;
    }

    public String getUserId() {
        return this.userId;
    }

    public String post(String url, Object body) throws IOException {
        String bodyAsJson = JsonUtil.toJson(body);
        Request request = new Request.Builder()
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bodyAsJson))
            .url(getTotalUrl(url)).build();
        Response response = requestContent(request);
        return toString(response.body());
    }

    public String put(String url, Object body) throws IOException {
        Request request = new Request.Builder()
            .put(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(body)))
            .url(getTotalUrl(url)).build();
        Response response = requestContent(request);
        return toString(response.body());
    }

    public String get(String url, Map<String, String> params) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getTotalUrl(url)).newBuilder();
        params.forEach(urlBuilder::addQueryParameter);

        Request request = new Request.Builder().get().url(urlBuilder.build()).build();
        Response response = requestContent(request);
        return toString(response.body());
    }

    public Response download(String url, HttpServletRequest httpServletRequest, long size) throws IOException {
        Request.Builder builder = new Request.Builder().header("referer", "https://www.aliyundrive.com/");
        String range = httpServletRequest.getHeader("range");
        if (range != null) {
            // 如果range最后 >= size， 则去掉
            String[] split = range.split("-");
            if (split.length == 2) {
                String end = split[1];
                if (Long.parseLong(end) >= size) {
                    range = range.substring(0, range.lastIndexOf('-') + 1);
                }
            }
            builder.header("range", range);
        }

        String ifRange = httpServletRequest.getHeader("if-range");
        if (ifRange != null) {
            builder.header("if-range", ifRange);
        }
        Request request = builder.url(url).build();
        return okHttpClient.newCall(request).execute();
    }

    public void upload(String url, byte[] bytes, final int offset, final int byteCount) throws IOException {
        Request request = new Request.Builder()
            .put(RequestBody.create(MediaType.parse(""), bytes, offset, byteCount))
            .url(url).build();
        okHttpClient.newCall(request).execute();
    }

    /**
     * 409, 频繁请求引起, 抛出IOException重试
     */
    private Response requestContent(Request request) throws IOException {
        Response response;
        try {
            response = okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new WebdavException(e);
        }
        if (!response.isSuccessful()) {
            log.warn(request.url() + ", 请求失败: " + response.message());
            if (response.code() == 429) {
                throw new IOException("请求失败");
            } else {
                throw new WebdavException("请求失败:" + response.message());
            }

        }
        return response;
    }

    private String toString(ResponseBody responseBody) throws IOException {
        if (responseBody == null) {
            return null;
        }
        return responseBody.string();
    }

    private String getTotalUrl(String url) {
        if (url.startsWith("http")) {
            return url;
        }
        return aliYunDriveProperties.getUrl() + url;
    }
}
