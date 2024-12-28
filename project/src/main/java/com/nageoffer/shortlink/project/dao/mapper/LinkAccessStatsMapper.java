package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkAccessStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 短链接基础访问监控持久层
 */
public interface LinkAccessStatsMapper extends BaseMapper<LinkAccessStatsDO> {

    @Insert("INSERT into t_link_access_stats (full_short_url, gid, date, pv, uv, uip, hour, weekday, create_time, update_time, del_flag)" +
    "values (#{fullShortUrl}, #{gid}, #{date}, #{pv}, #{uv}, #{uip}, #{hour}, #{weekday},NOW(),NOW(), 0)" +
            "ON DUPLICATE KEY UPDATE pv = pv + #{pv}," +
            "uv = uv + #{uv}, uip = uip + #{uip};")
//    void shortLinkStats(@Param("linkAccessStats") LinkAccessStatsDO linkAccessStatsDO);
    void shortLinkStats(LinkAccessStatsDO linkAccessStatsDO);
}