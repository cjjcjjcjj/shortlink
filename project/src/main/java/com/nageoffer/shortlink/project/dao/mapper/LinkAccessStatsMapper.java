package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    /**
     * 根据短链接获取指定日期内基础监控数据
     */
    @Select("""
        select date, SUM(pv) as pv, SUM(uv) AS uv, SUM(uip) AS uip from t_link_access_stats
        where full_short_url = #{fullShortUrl} and gid = #{gid} and date BETWEEN #{startDate} and #{endDate}
        group by full_short_url, gid, date;
""")
    List<LinkAccessStatsDO> listStatsByShortLink(ShortLinkStatsReqDTO requestParam);

    /**
     * 根据短链接获取指定日期内小时基础监控数据
     */
    @Select("""
        select hour, SUM(pv) as pv from t_link_access_stats
        where full_short_url = #{fullShortUrl} and gid = #{gid} and date BETWEEN #{startDate} and #{endDate}
        group by full_short_url, gid, hour;
""")
    List<LinkAccessStatsDO> listHourStatsByShortLink(ShortLinkStatsReqDTO requestParam);

    /**
     * 根据短链接获取指定日期内星期基础监控数据
     */
    @Select("""
        select weekday, SUM(pv) as pv from t_link_access_stats
        where full_short_url = #{fullShortUrl} and gid = #{gid} and date BETWEEN #{startDate} and #{endDate}
        group by full_short_url, gid, weekday;
""")
    List<LinkAccessStatsDO> listWeekdayStatsByShortLink(ShortLinkStatsReqDTO requestParam);

    /**
     * 根据分组获取指定日期内基础监控数据
     */
    @Select("SELECT " +
            "    date, " +
            "    SUM(pv) AS pv, " +
            "    SUM(uv) AS uv, " +
            "    SUM(uip) AS uip " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    gid = #{gid} " +
            "    AND date BETWEEN #{startDate} and #{endDate} " +
            "GROUP BY " +
            "    gid, date;")
    List<LinkAccessStatsDO> listStatsByGroup(ShortLinkGroupStatsReqDTO requestParam);

    /**
     * 根据分组获取指定日期内小时基础监控数据
     */
    @Select("SELECT " +
            "    hour, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    gid = #{gid} " +
            "    AND date BETWEEN #{startDate} and #{endDate} " +
            "GROUP BY " +
            "    gid, hour;")
    List<LinkAccessStatsDO> listHourStatsByGroup(ShortLinkGroupStatsReqDTO requestParam);

    /**
     * 根据分组获取指定日期内星期基础监控数据
     */
    @Select("SELECT " +
            "    weekday, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    gid = #{gid} " +
            "    AND date BETWEEN #{startDate} and #{endDate} " +
            "GROUP BY " +
            "    gid, weekday;")
    List<LinkAccessStatsDO> listWeekdayStatsByGroup(ShortLinkGroupStatsReqDTO requestParam);
}
