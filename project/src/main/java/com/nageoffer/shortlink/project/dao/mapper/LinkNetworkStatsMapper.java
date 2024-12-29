package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkNetworkStatsDO;
import org.apache.ibatis.annotations.Insert;

/**
 * 记录网络访问持久层
 */
public interface LinkNetworkStatsMapper extends BaseMapper<LinkNetworkStatsDO> {

    /**
     * 记录网络访问
     * @param linkNetworkStatsDO
     */
    @Insert("""
            INSERT INTO t_link_network_stats (full_short_url, gid, date, cnt, network, create_time, update_time, del_flag)
        VALUES (#{fullShortUrl}, #{gid}, #{date}, #{cnt}, #{network}, NOW(), NOW(), 0)
        ON DUPLICATE KEY UPDATE
           cnt = cnt + #{cnt}
""")
    void shortLinkNetworkStats(LinkNetworkStatsDO linkNetworkStatsDO);
}
