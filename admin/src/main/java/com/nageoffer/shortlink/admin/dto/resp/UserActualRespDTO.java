package com.nageoffer.shortlink.admin.dto.resp;

import lombok.Data;

/**
 * 用户返回参数实体
 */
@Data
public class UserActualRespDTO {
    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String mail;

}
