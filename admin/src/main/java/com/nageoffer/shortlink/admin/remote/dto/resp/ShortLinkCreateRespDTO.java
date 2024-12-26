package com.nageoffer.shortlink.admin.remote.dto.resp;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接创建响应对象
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShortLinkCreateRespDTO {
    //返回一个分组信息

    /**
     * 分组标识
     */
    private String gid;
    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

}