package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkLocateStatsDO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    /**
     * 根据短链接获取指定日期内地区监控数据
     */
    @Select("SELECT " +
            "    province, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_locate_stats " +
            "WHERE " +
            "    full_short_url = #{fullShortUrl} " +
            "    AND gid = #{gid} " +
            "    AND date BETWEEN #{startDate} and #{endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, province;")
    List<LinkLocateStatsDO> listLocateByShortLink(ShortLinkStatsReqDTO requestParam);
}
