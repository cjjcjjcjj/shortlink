package com.nageoffer.shortlink.project.dto.req;


import lombok.Data;

/**
 * 回收站彻底删除功能
 */
@Data
public class RecycleBinRemoveReqDTO {

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;
}
