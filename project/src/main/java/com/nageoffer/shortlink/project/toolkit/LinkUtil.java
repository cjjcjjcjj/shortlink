package com.nageoffer.shortlink.project.toolkit;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import jakarta.servlet.http.HttpServletRequest;
import ua_parser.Client;
import ua_parser.Parser;

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

    /**
     * 获取请求的 IP 地址
     * @param request
     * @return
     */
    public static String getIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        return ipAddress;
    }

    /**
     * 获取用户访问操作系统
     *
     * @param request 请求
     * @return 访问操作系统
     */
    public static String getOs(HttpServletRequest request){
        String userAgent = request.getHeader("User-Agent");
        Parser parser = new Parser();
        Client client = parser.parse(userAgent);
        return client.os.family; // 返回操作系统类型
    }

    /**
     * 解析用户的浏览器并返回
     * @param request
     * @return
     */
    public static String getBrowser(HttpServletRequest request){
        String userAgent = request.getHeader("User-Agent");
        Parser parser = new Parser();
        Client client = parser.parse(userAgent);
        return client.userAgent.family; // 返回浏览器类型
    }

}
