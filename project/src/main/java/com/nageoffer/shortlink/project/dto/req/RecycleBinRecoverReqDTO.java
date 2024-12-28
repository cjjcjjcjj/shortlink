package com.nageoffer.shortlink.project.dto.req;


import lombok.Data;

/**
 * 回收站恢复功能
 */
@Data
public class RecycleBinRecoverReqDTO {

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;
}
