package com.nageoffer.shortlink.project.toolkit;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.Optional;

import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.*;

/**
 * 短链接工具类
 */
public class LinkUtil {

    /**
     * 获取短链接有效期时间
     * @param vaildDate
     * @return 有效期时间戳
     */
    public static  long getLinkCacheValidTime(Date vaildDate){
        return Optional.ofNullable(vaildDate)
                .map(each -> DateUtil.between(new Date(), each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }
}
