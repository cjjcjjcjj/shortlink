package com.nageoffer.shortlink.admin.remote.dto.resp;

import lombok.Data;

/**
 * 短链接分组查询返回参数参数
 */
@Data
public class ShortLinkGroupCountQueryRespDTO {

    /**
     * 分组id标识
     */
    private String gid;

    /**
     * 短链接数量
     */
    private Integer shortLinkCount;
}
