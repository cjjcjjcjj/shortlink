package com.nageoffer.shortlink.project.dto.req;


import lombok.Data;

/**
 * 回收站保存功能
 */
@Data
public class RecycleBinSaveReqDTO {

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;
}
