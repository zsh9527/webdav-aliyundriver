package com.github.webdavteambition.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.webdavteambition.constant.AliDriverConstants;
import com.github.webdavteambition.model.*;
import com.github.webdavteambition.model.result.*;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.webdav.exceptions.WebdavException;
import okhttp3.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云Api请求client
 */
@Component
@Slf4j
@Lazy
@RequiredArgsConstructor
public class AliYunDriverClient {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;


    /**
     * 获取token
     */
    @Retry(name = "retry-backend")
    public TokenResp getToken(String refreshToken) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.AUTH_HOSTNAME + "/v2/account/token");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(params));
        requestBuilder.post(requestBody);
        try {
            return requestContent(requestBuilder.build(), TokenResp.class);
        } catch (Exception e) {
            // 失败重试一次
            return requestContent(requestBuilder.build(), TokenResp.class);
        }
    }

    /**
     * 获取用户信息
     */
    @Retry(name = "retry-backend")
    public UserInfoResp getUserInfo() throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/v2/user/get");
        RequestBody requestBody = RequestBody.create("{}".getBytes());
        requestBuilder.post(requestBody);
        return requestContent(requestBuilder.build(), UserInfoResp.class);
    }

    /**
     * 签名
     */
    @Retry(name = "retry-backend")
    public boolean createSession(String xDeviceId, String publicKey, String signatureData) throws IOException {
        // 使用 ECKeyPair 对象进行签名
        var body = Map.of(
            "deviceName", "Edge浏览器",
            "modelName", "Windows网页版",
            "pubKey", "04" + publicKey
        );
        Request.Builder requestBuilder = new Request.Builder()
            .header("x-canary", "client=web,app=adrive,version=v4.3.1")
            .header("x-device-id", xDeviceId)
            .header("x-signature", signatureData)
            .url(AliDriverConstants.HOSTNAME + "/users/v1/users/device/create_session");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(body));
        requestBuilder.post(requestBody);
        String content = requestContent(requestBuilder.build()).body().string();
        if (content.contains(",\"success\":true")) {
            log.info("公私钥验证成功");
            return true;
        }
        throw new WebdavException("签名失败:" + content);
    }

    /**
     * list
     */
    @Retry(name = "retry-backend")
    public TFileListResult<TFile> listFile(FileListRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/adrive/v3/file/list");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        Response response = requestContent(requestBuilder.build());
        String responseContent = response.body().string();
        TFileListResult<TFile> tFileListResult = objectMapper.readValue(responseContent, new TypeReference<>() {
        });
        return tFileListResult;
    }

    @Retry(name = "retry-backend")
    public void renameFile(RenameRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/v2/file/update");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        requestContent(requestBuilder.build());
    }

    @Retry(name = "retry-backend")
    public void move(MoveRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/v2/file/move");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        requestContent(requestBuilder.build());
    }

    @Retry(name = "retry-backend")
    public void remove(RemoveRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/v2/recyclebin/trash");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        requestContent(requestBuilder.build());
    }

    @Retry(name = "retry-backend")
    public TFile createFolder(CreateFileRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/adrive/v2/file/createWithFolders");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        return requestContent(requestBuilder.build(), TFile.class);
    }

    @Retry(name = "retry-backend")
    public UploadPreResult createFile(UploadPreRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/adrive/v2/file/createWithFolders");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        return requestContent(requestBuilder.build(), UploadPreResult.class);
    }

    @Retry(name = "retry-backend")
    public void complete(UploadFinalRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/v2/file/complete");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        requestContent(requestBuilder.build());
    }

    @Retry(name = "retry-backend")
    public UploadPreResult getUploadUrl(RefreshUploadUrlRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/v2/file/get_upload_url");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        return requestContent(requestBuilder.build(), UploadPreResult.class);
    }

    @Retry(name = "retry-backend")
    public DownloadUrlResp getDownloadUrl(DownloadRequest request) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
            .url(AliDriverConstants.HOSTNAME + "/v2/file/get_download_url");
        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsBytes(request));
        requestBuilder.post(requestBody);
        return requestContent(requestBuilder.build(), DownloadUrlResp.class);
    }

    @Retry(name = "retry-backend")
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
        return  requestContent(request);
    }

    @Retry(name = "retry-backend")
    public void upload(String url, byte[] bytes, final int offset, final int byteCount) throws IOException {
        Request request = new Request.Builder()
            .put(RequestBody.create(MediaType.parse(""), bytes, offset, byteCount))
            .url(url).build();
        requestContent(request);
    }

    private <T> T requestContent(Request request, Class<T> tClass) throws IOException {
        Response response = requestContent(request);
        return objectMapper.readValue(response.body().byteStream(), tClass);
    }

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
}
