package com.nageoffer.shortlink.project.common.constant;

/**
 * Redis Key常量类
 */
public class RedisKeyConstant {

    /**
     * 短链接跳转前缀 Key
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link:goto:%s";

    /**
     * 短链接跳转锁前缀 Key
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link:lock:goto:%s";

    /**
     * 短链接跳转空值前缀 Key
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link:is-null:goto:%s";


    /**
     * 短链接修改分组 ID 锁前缀 Key
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link:lock:update-gid:%s";

    /**
     * 短链接延迟队列消费统计 Key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link:delay-queue:stats";

    /**
     * 短链接统计判断是否新用户缓存
     */
    public static final String SHORT_LINK_STATES_UV_KEY = "short-link:stats:uv:";

    /**
     * 短链接统计判断是否新IP缓存
     */
    public static final String SHORT_LINK_STATES_UIP_KEY = "short-link:stats:uip:";

}
