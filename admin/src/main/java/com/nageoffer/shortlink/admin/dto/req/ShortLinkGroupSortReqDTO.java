package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

/**
 * 短链接分组创建参数
 */
@Data
public class ShortLinkGroupSortReqDTO {

    /**
     * 短链接分组id
     */
    private String gid;

    /**
     *
     */
    private Integer sortOrder;
}
