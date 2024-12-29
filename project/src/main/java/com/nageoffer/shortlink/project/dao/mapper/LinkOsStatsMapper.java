package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkLocateStatsDO;
import com.nageoffer.shortlink.project.dao.entity.LinkOsStatsDO;
import org.apache.ibatis.annotations.Insert;


/**
 * 短链接操作系统监控持久层
 */
public interface LinkOsStatsMapper extends BaseMapper<LinkOsStatsDO> {

    /**
     * 记录操作系统访问
     * @param linkOsStatsDO
     */
    @Insert("""
            INSERT INTO t_link_os_stats (full_short_url, gid, date, cnt, os, create_time, update_time, del_flag)
        VALUES (#{fullShortUrl}, #{gid}, #{date}, #{cnt}, #{os}, NOW(), NOW(), 0)
        ON DUPLICATE KEY UPDATE
           cnt = cnt + #{cnt}
""")
    void shortLinOsStats(LinkOsStatsDO linkOsStatsDO);
}
