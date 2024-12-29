package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkLocateStatsDO;
import org.apache.ibatis.annotations.Insert;

/**
 * 短链接地区监控持久层
 */
public interface LinkLocateStatsMapper extends BaseMapper<LinkLocateStatsDO> {


    /**
     * 记录地区访问
     * @param linkLocateStatsDO
     */
    @Insert("""
            INSERT INTO t_link_locate_stats (full_short_url, gid, date, cnt, province, city, adcode, country, create_time, update_time, del_flag)
        VALUES (#{fullShortUrl}, #{gid}, #{date}, #{cnt}, #{province}, #{city}, #{adcode}, #{country}, NOW(), NOW(), 0)
        ON DUPLICATE KEY UPDATE
           cnt = cnt + #{cnt}
""")
    void shortLinkLocateStats(LinkLocateStatsDO linkLocateStatsDO);
}