package com.github.webdavteambition.model.result;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TokenResp {
    private String access_token;
    private String refresh_token;
}
