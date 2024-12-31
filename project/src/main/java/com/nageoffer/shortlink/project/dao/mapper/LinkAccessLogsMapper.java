package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.project.dao.entity.LinkAccessLogsDO;
import com.nageoffer.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 访问日志监控持久层
 */
public interface LinkAccessLogsMapper extends BaseMapper<LinkAccessLogsDO> {

    /**
     * 根据短链接获取指定日期内高频访问IP数据
     */
    @Select("""
    select ip, COUNT(ip) AS count from t_link_access_logs
    where full_short_url = #{fullShortUrl} AND gid = #{gid} AND create_time BETWEEN CONCAT(#{startDate},' 00:00:00') and
        CONCAT(#{endDate},' 23:59:59') group by full_short_url, gid, ip order by count DESC LIMIT 5;
""")
    List<HashMap<String, Object>> listTopIpByShortLink(ShortLinkStatsReqDTO requestParam);


    /**
     * 根据分组获取指定日期内高频访问IP数据
     */
    @Select("SELECT " +
            "    ip, " +
            "    COUNT(ip) AS count " +
            "FROM " +
            "    t_link_access_logs " +
            "WHERE " +
            "    gid = #{gid} " +
            "    AND create_time BETWEEN #{startDate} and #{endDate} " +
            "GROUP BY " +
            "    gid, ip " +
            "ORDER BY " +
            "    count DESC " +
            "LIMIT 5;")
    List<HashMap<String, Object>> listTopIpByGroup(ShortLinkGroupStatsReqDTO requestParam);


    /**
     * 根据短链接获取指定日期内新旧访客数据
     */
    @Select("SELECT " +
            "    SUM(old_user) AS oldUserCnt, " +
            "    SUM(new_user) AS newUserCnt " +
            "FROM ( " +
            "    SELECT " +
            "        CASE WHEN COUNT(DISTINCT DATE(create_time)) > 1 THEN 1 ELSE 0 END AS old_user, " +
            "        CASE WHEN COUNT(DISTINCT DATE(create_time)) = 1 AND MAX(create_time) >= #{startDate} AND MAX(create_time) <= #{endDate} THEN 1 ELSE 0 END AS new_user " +
            "    FROM " +
            "        t_link_access_logs " +
            "    WHERE " +
            "        full_short_url = #{fullShortUrl} " +
            "        AND gid = #{gid} " +
            "    GROUP BY " +
            "        user " +
            ") AS user_counts;")
    HashMap<String, Object> findUvTypeCntByShortLink(ShortLinkStatsReqDTO requestParam);

    /**
     * 根据用户信息获取是否新老访客
     * @param gid
     * @param fullShortUrl
     * @param startDate
     * @param endDate
     * @param userAccessLogsList
     * @return
     */
    @Select("<script> " +
            "SELECT " +
            "    user, " +
            "    CASE " +
            "        WHEN MIN(create_time) BETWEEN #{startDate} AND #{endDate} THEN '新访客' " +
            "        ELSE '老访客' " +
            "    END AS uvType " +
            "FROM " +
            "    t_link_access_logs " +
            "WHERE " +
            "    full_short_url = #{fullShortUrl} " +
            "    AND gid = #{gid} " +
            "    AND user IN " +
            "    <foreach item='item' index='index' collection='userAccessLogsList' open='(' separator=',' close=')'> " +
            "        #{item} " +
            "    </foreach> " +
            "GROUP BY " +
            "    user;" +
            "    </script>"
    )
    List<Map<String, Object>> selectUvTypeByUsers(
            @Param("gid") String gid,
            @Param("fullShortUrl") String fullShortUrl,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("userAccessLogsList") List<String> userAccessLogsList
    );

    /**
     * 根据短链接获取指定日期内pv，uv，uip
     * @param requestParam
     * @return
     */
    @Select("""
select COUNT(user) as pv, COUNT(DISTINCT user) as uv, COUNT(DISTINCT ip) as uip from t_link_access_logs
where full_short_url = #{fullShortUrl} AND gid = #{gid} AND
create_time BETWEEN CONCAT(#{startDate},' 00:00:00') AND CONCAT(#{endDate},' 23:59:59')
GROUP By full_short_url, gid;
""")
    LinkAccessStatsDO findPvUvUipStatsByShortLink(ShortLinkStatsReqDTO requestParam);

    /**
     * 根据分组获取指定日期内PV、UV、UIP数据
     */
    @Select("SELECT " +
            "    COUNT(user) AS pv, " +
            "    COUNT(DISTINCT user) AS uv, " +
            "    COUNT(DISTINCT ip) AS uip " +
            "FROM " +
            "    t_link_access_logs " +
            "WHERE " +
            "    gid = #{gid} " +
            "    AND create_time BETWEEN CONCAT(#{startDate},' 00:00:00') AND CONCAT(#{endDate},' 23:59:59') " +
            "GROUP BY " +
            "    gid;")
    LinkAccessStatsDO findPvUvUidStatsByGroup(ShortLinkGroupStatsReqDTO requestParam);
}
