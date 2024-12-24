package com.nageoffer.shortlink.project.dto.resp;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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
