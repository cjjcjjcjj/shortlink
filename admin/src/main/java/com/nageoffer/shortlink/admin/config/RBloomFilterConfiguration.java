package com.nageoffer.shortlink.admin.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 */
@Configuration
public class RBloomFilterConfiguration {

    /**
     * 防止用户注册查询数据库的布隆过滤器
     * tryInit 有两个核心参数：
     * ● expectedInsertions：预估布隆过滤器存储的元素长度。
     * ● falseProbability：运行的误判率。
     * 错误率越低，位数组越长，布隆过滤器的内存占用越大。
     * 错误率越低，散列 Hash 函数越多，计算耗时较长。
     * 一个布隆过滤器占用大小的在线网站：https://krisives.github.io/bloom-calculator/
     * 使用布隆过滤器的两种场景：
     * ● 初始使用：注册用户时就向容器中新增数据，就不需要任务向容器存储数据了。
     * ● 使用过程中引入：读取数据源将目标数据刷到布隆过滤器。
     */
    @Bean
    public RBloomFilter<String> userRegisterCachePenetrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter("userRegisterCachePenetrationBloomFilter");
        cachePenetrationBloomFilter.tryInit(100000000L, 0.001);
        return cachePenetrationBloomFilter;
    }
}
