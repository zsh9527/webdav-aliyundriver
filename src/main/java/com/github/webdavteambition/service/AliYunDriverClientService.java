package com.github.webdavteambition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.webdavteambition.client.AliYunDriverClient;
import com.github.webdavteambition.config.AliYunClientProperties;
import com.github.webdavteambition.model.*;
import com.github.webdavteambition.model.result.TFile;
import com.github.webdavteambition.model.result.TFileListResult;
import com.github.webdavteambition.model.result.UploadPreResult;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.sf.webdav.exceptions.WebdavException;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AliYunDriverClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverClientService.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static String rootPath = "/";
    private static int chunkSize = 10485760; // 10MB
    private TFile rootTFile;

    private static Cache<String, Set<TFile>> tFilesCache = Caffeine.newBuilder()
            .initialCapacity(128)
            .maximumSize(1024)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    private final AliYunDriverClient aliYunDriverClient;
    private final VirtualTFileService virtualTFileService;
    private final AliYunClientProperties aliYunClientProperties;

    @PostConstruct
    void init() {
        AliYunDriverFileSystemStore.setBean(this);
        rootTFile = new TFile();
        rootTFile.setName("/");
        rootTFile.setFile_id("root");
        rootTFile.setCreated_at(new Date());
        rootTFile.setUpdated_at(new Date());
        rootTFile.setType("folder");
    }

    public Set<TFile> getTFiles(String nodeId) {
        Set<TFile> tFiles = tFilesCache.get(nodeId, key -> {
            // 获取真实的文件列表
            return getTFiles2(nodeId);
        });
        Set<TFile> all = new LinkedHashSet<>(tFiles);
        // 获取上传中的文件列表
        Collection<TFile> virtualTFiles = virtualTFileService.list(nodeId);
        all.addAll(virtualTFiles);
        return all;
    }

    private Set<TFile> getTFiles2(String nodeId) {
        List<TFile> tFileList = fileListFromApi(nodeId, null, new ArrayList<>());
        tFileList.sort(Comparator.comparing(TFile::getUpdated_at).reversed());
        Set<TFile> tFileSets = new LinkedHashSet<>();
        for (TFile tFile : tFileList) {
            if (!tFileSets.add(tFile)) {
                LOGGER.info("当前目录下{} 存在同名文件：{}，文件大小：{}", nodeId, tFile.getName(), tFile.getSize());
            }
        }
        // 对文件名进行去重，只保留最新的一个
        return tFileSets;
    }

    @SneakyThrows
    private List<TFile> fileListFromApi(String nodeId, String marker, List<TFile> all) {
        FileListRequest listQuery = new FileListRequest();
        listQuery.setMarker(marker);
        listQuery.setDrive_id(aliYunClientProperties.getDriveId());
        listQuery.setParent_file_id(nodeId);
        TFileListResult<TFile> tFileListResult = aliYunDriverClient.listFile(listQuery);
        all.addAll(tFileListResult.getItems());
        if (!StringUtils.hasLength(tFileListResult.getNext_marker())) {
            return all;
        }
        return fileListFromApi(nodeId, tFileListResult.getNext_marker(), all);
    }

    /**
     * 创建文件 | 修改文件内容
     */
    @SneakyThrows
    public void uploadPre(String path, long size, InputStream inputStream) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        TFile parent = getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            return;
        }
        // 如果已存在，先删除
        TFile tfile = getTFileByPath(path);
        if (tfile != null) {
            if (tfile.getSize() == size) {
                //如果文件大小一样，则不再上传
                return;
            }
            remove(path);
        }


        int chunkCount = (int) Math.ceil(((double) size) / chunkSize); // 进1法

        UploadPreRequest uploadPreRequest = new UploadPreRequest();
//        uploadPreRequest.setContent_hash(UUID.randomUUID().toString());
        uploadPreRequest.setDrive_id(aliYunClientProperties.getDriveId());
        uploadPreRequest.setName(pathInfo.getName());
        uploadPreRequest.setParent_file_id(parent.getFile_id());
        uploadPreRequest.setSize(size);
        List<UploadPreRequest.PartInfo> part_info_list = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            UploadPreRequest.PartInfo partInfo = new UploadPreRequest.PartInfo();
            partInfo.setPart_number(i + 1);
            part_info_list.add(partInfo);
        }
        uploadPreRequest.setPart_info_list(part_info_list);

        LOGGER.info("开始上传文件，文件名：{}，总大小：{}, 文件块数量：{}", path, size, chunkCount);

        UploadPreResult uploadPreResult = aliYunDriverClient.createFile(uploadPreRequest);
        List<UploadPreRequest.PartInfo> partInfoList = uploadPreResult.getPart_info_list();
        if (partInfoList != null) {
            if (size > 0) {
                virtualTFileService.createTFile(parent.getFile_id(), uploadPreResult);
            }
            LOGGER.info("文件预处理成功，开始上传。文件名：{}，上传URL数量：{}", path, partInfoList.size());

            byte[] buffer = new byte[chunkSize];
            for (int i = 0; i < partInfoList.size(); i++) {
                UploadPreRequest.PartInfo partInfo = partInfoList.get(i);

                long expires = Long.parseLong(Objects.requireNonNull(Objects.requireNonNull(HttpUrl.parse(partInfo.getUpload_url())).queryParameter("x-oss-expires")));
                if (System.currentTimeMillis() / 1000 + 10 >= expires) {
                    // 已过期，重新置换UploadUrl
                    RefreshUploadUrlRequest refreshUploadUrlRequest = new RefreshUploadUrlRequest();
                    refreshUploadUrlRequest.setDrive_id(aliYunClientProperties.getDriveId());
                    refreshUploadUrlRequest.setUpload_id(uploadPreResult.getUpload_id());
                    refreshUploadUrlRequest.setFile_id(uploadPreResult.getFile_id());
                    refreshUploadUrlRequest.setPart_info_list(part_info_list);
                    UploadPreResult refreshResult = aliYunDriverClient.getUploadUrl(refreshUploadUrlRequest);
                    for (int j = i; j < partInfoList.size(); j++) {
                        UploadPreRequest.PartInfo oldInfo = partInfoList.get(j);
                        UploadPreRequest.PartInfo newInfo = refreshResult.getPart_info_list().stream().filter(p -> p.getPart_number().equals(oldInfo.getPart_number())).findAny().orElseThrow(NullPointerException::new);
                        oldInfo.setUpload_url(newInfo.getUpload_url());
                    }
                }

                try {
                    int read = IOUtils.read(inputStream, buffer, 0, buffer.length);
                    if (read == -1) {
                        LOGGER.info("文件上传结束。文件名：{}，当前进度：{}/{}", path, (i + 1), partInfoList.size());
                        return;
                    }
                    aliYunDriverClient.upload(partInfo.getUpload_url(), buffer, 0, read);
                    virtualTFileService.updateLength(parent.getFile_id(), uploadPreResult.getFile_id(), buffer.length);
                    LOGGER.info("文件正在上传。文件名：{}，当前进度：{}/{}", path, (i + 1), partInfoList.size());
                } catch (IOException e) {
                    virtualTFileService.remove(parent.getFile_id(), uploadPreResult.getFile_id());
                    throw new WebdavException(e);
                }
            }
        }



        UploadFinalRequest uploadFinalRequest = new UploadFinalRequest();
        uploadFinalRequest.setFile_id(uploadPreResult.getFile_id());
        uploadFinalRequest.setDrive_id(aliYunClientProperties.getDriveId());
        uploadFinalRequest.setUpload_id(uploadPreResult.getUpload_id());

        aliYunDriverClient.complete(uploadFinalRequest);
        virtualTFileService.remove(parent.getFile_id(), uploadPreResult.getFile_id());
        LOGGER.info("文件上传成功。文件名：{}", path);
        clearCache();
    }


    @SneakyThrows
    public void rename(String sourcePath, String newName) {
        sourcePath = normalizingPath(sourcePath);
        TFile tFile = getTFileByPath(sourcePath);
        RenameRequest renameRequest = new RenameRequest();
        renameRequest.setDrive_id(aliYunClientProperties.getDriveId());
        renameRequest.setFile_id(tFile.getFile_id());
        renameRequest.setName(newName);
        aliYunDriverClient.renameFile(renameRequest);
        clearCache();
    }

    @SneakyThrows
    public void move(String sourcePath, String targetPath) {
        sourcePath = normalizingPath(sourcePath);
        targetPath = normalizingPath(targetPath);

        TFile sourceTFile = getTFileByPath(sourcePath);
        TFile targetTFile = getTFileByPath(targetPath);
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setDrive_id(aliYunClientProperties.getDriveId());
        moveRequest.setFile_id(sourceTFile.getFile_id());
        moveRequest.setTo_parent_file_id(targetTFile.getFile_id());
        aliYunDriverClient.move(moveRequest);
        clearCache();
    }

    /**
     * TODO 删除文件太多, 可使用批处理任务
     */
    @SneakyThrows
    public void remove(String path) {
        path = normalizingPath(path);
        TFile tFile = getTFileByPath(path);
        if (tFile == null) {
            return;
        }
        RemoveRequest removeRequest = new RemoveRequest();
        removeRequest.setDrive_id(aliYunClientProperties.getDriveId());
        removeRequest.setFile_id(tFile.getFile_id());
        aliYunDriverClient.remove(removeRequest);
        clearCache();
    }

    @SneakyThrows
    public void createFolder(String path) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        TFile parent =  getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            LOGGER.warn("创建目录失败，未发现父级目录：{}", pathInfo.getParentPath());
            return;
        }
        CreateFileRequest createFileRequest = new CreateFileRequest();
        createFileRequest.setDrive_id(aliYunClientProperties.getDriveId());
        createFileRequest.setName(pathInfo.getName());
        createFileRequest.setParent_file_id(parent.getFile_id());
        createFileRequest.setType(FileType.folder.name());
        TFile createFileResult = aliYunDriverClient.createFolder(createFileRequest);
        if (createFileResult.getFile_name() == null) {
            LOGGER.error("创建目录{}失败", path);
        }
        if (!createFileResult.getFile_name().equals(pathInfo.getName())) {
            LOGGER.info("创建目录{}与原值{}不同，重命名", createFileResult.getName(), pathInfo.getName());
            rename(pathInfo.getParentPath() + "/" + createFileResult.getName(), pathInfo.getName());
            clearCache();
        }
        clearCache();
    }


    public TFile getTFileByPath(String path) {
        path = normalizingPath(path);

        return getNodeIdByPath2(path);
    }

    @SneakyThrows
    public Response download(String path, HttpServletRequest request, long size ) {
        TFile file = getTFileByPath(path);
        DownloadRequest downloadRequest = new DownloadRequest();
        downloadRequest.setDrive_id(aliYunClientProperties.getDriveId());
        downloadRequest.setFile_id(file.getFile_id());
        String url = aliYunDriverClient
            .getDownloadUrl(downloadRequest, aliYunClientProperties.getXDeviceId(), aliYunClientProperties.getXSignature())
            .getUrl();
        LOGGER.debug("{} url = {}", path, url);
        return aliYunDriverClient.download(url, request, size);
    }

    private TFile getNodeIdByPath2(String path) {
        if (!StringUtils.hasLength(path) || path.equals(rootPath)) {
            return this.rootTFile;
        }
        PathInfo pathInfo = getPathInfo(path);
        TFile tFile = getTFileByPath(pathInfo.getParentPath());
        if (tFile == null ) {
            return null;
        }
        return getNodeIdByParentId(tFile.getFile_id(), pathInfo.getName());
    }


    public PathInfo getPathInfo(String path) {
        path = normalizingPath(path);
        if (path.equals(rootPath)) {
            PathInfo pathInfo = new PathInfo();
            pathInfo.setPath(path);
            pathInfo.setName(path);
            return pathInfo;
        }
        int index = path.lastIndexOf("/");
        String parentPath = path.substring(0, index + 1);
        String name = path.substring(index+1);
        PathInfo pathInfo = new PathInfo();
        pathInfo.setPath(path);
        pathInfo.setParentPath(parentPath);
        pathInfo.setName(name);
        return pathInfo;
    }

    private TFile getNodeIdByParentId(String parentId, String name) {
        Set<TFile> tFiles = getTFiles(parentId);
        for (TFile tFile : tFiles) {
            if (tFile.getName().equals(name)) {
                return tFile;
            }
        }
        return null;
    }


    private String normalizingPath(String path) {
        path = path.replaceAll("//", "/");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void clearCache() {
        tFilesCache.invalidateAll();
    }
}
