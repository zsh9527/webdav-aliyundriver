package com.github.webdavteambition.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class FileListRequest extends Page {
    private String drive_id;
    private Boolean all = false;
    private String fields = "*";
    private String image_thumbnail_process = "image/resize,w_256/format,jpeg";
    private String image_url_process = "image/resize,w_1920/format,jpeg/interlace,1";
    private String parent_file_id;
    private String video_thumbnail_process = "video/snapshot,t_1000,f_jpg,ar_auto,w_256";
    private int url_expire_sec = 14400;
}
