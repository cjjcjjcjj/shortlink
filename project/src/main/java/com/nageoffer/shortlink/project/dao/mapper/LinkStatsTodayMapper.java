package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkOsStatsDO;
import com.nageoffer.shortlink.project.dao.entity.LinkStatsTodayDO;
import org.apache.ibatis.annotations.Insert;

/**
 * 短链接今日统计持久层
 */
public interface LinkStatsTodayMapper extends BaseMapper<LinkStatsTodayDO> {

    /**
     * 记录操作系统访问
     * @param linkStatsTodayDO
     */
    @Insert("""
            INSERT INTO t_link_stats_today (gid, full_short_url, date, today_pv, today_uv, today_uip, create_time, update_time, del_flag)
        VALUES (#{gid}, #{fullShortUrl}, #{date}, #{todayPv}, #{todayUv}, #{todayUip}, NOW(), NOW(), 0)
        ON DUPLICATE KEY UPDATE
           today_uv = today_uv + #{todayUv},
           today_pv = today_pv + #{todayPv},
           today_uip = today_uip;
""")
    void shortLinTodayStats(LinkStatsTodayDO linkStatsTodayDO);
}
