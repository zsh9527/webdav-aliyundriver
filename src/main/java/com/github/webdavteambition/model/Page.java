package com.github.webdavteambition.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Page {
    private String marker;
    private int limit = 100;
    private String order_by = "updated_at";
    private String order_direction = "DESC";
}
