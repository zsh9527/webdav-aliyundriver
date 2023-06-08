package com.github.webdavteambition.config;

import com.github.webdavteambition.model.result.TFile;
import com.github.webdavteambition.service.AliYunDriverClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AliYunDriverCronTask {

    @Autowired
    private AliYunDriverClientService aliYunDriverClientService;

    /**
     * 每隔5分钟请求一下接口，保证token不过期 TODO
     */
    @Scheduled(initialDelay = 30 * 1000, fixedDelay = 5 * 60 * 1000)
    public void refreshToken() {
        try {
            TFile root = aliYunDriverClientService.getTFileByPath("/");
            aliYunDriverClientService.getTFiles(root.getFile_id());
        } catch (Exception e) {
            // nothing
        }
    }
}
