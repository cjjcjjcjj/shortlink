package com.nageoffer.shortlink.project.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.VailDateTypeEnum;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.toolkit.HashUtil;
import com.nageoffer.shortlink.project.toolkit.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.distsql.parser.autogen.KernelDistSQLStatementParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;
import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;
import static com.nageoffer.shortlink.project.toolkit.LinkUtil.*;

/**
 * 短链接接口实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
//    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocateStatsMapper linkLocateStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;

    @Value("${short-link.stats.locate.AmMap-key}")
    private String AMapKey;

    @SneakyThrows
    @Override
    public void restore(String shortUri, HttpServletRequest request, HttpServletResponse response) {
        //TODO Redis操作，管道命令？
        String serverName = request.getServerName();
        String fullShortUrl = serverName + "/" + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)){
            shortLinkStats(fullShortUrl, null, request, response);
            response.sendRedirect(originalLink);
            return;
        }
        //缓存穿透
        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contains){
            response.sendRedirect("/page/notfound");
            return;
        }
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)){
            response.sendRedirect("/page/notfound");
            return;
        }
        //用户只能传短链接，没法传gid，需要一个路由表，后面改查redis了，避免查数据库
        //存在缓存击穿问题, 这个key刚好失效，但是查询请求大量到来，就会频繁访问数据库，造成缓存击穿
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {
            //TODO 双重判定锁？
            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originalLink)){
                shortLinkStats(fullShortUrl, null, request, response);
                response.sendRedirect(originalLink);
            }else {
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                if (shortLinkGotoDO == null){
                    // TODO 风控
                    stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                    response.sendRedirect("/page/notfound");
                    return;
                }
                LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                        .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                        .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                        .eq(ShortLinkDO::getEnableStatus, 0)
                        .eq(ShortLinkDO::getDelFlag, 0);
                ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
                if (shortLinkDO == null || (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date()))){
                    stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                    response.sendRedirect("/page/notfound");
                    return;
                }
                stringRedisTemplate.opsForValue().set(
                        String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                        shortLinkDO.getOriginUrl(),
                        getLinkCacheValidTime(shortLinkDO.getValidDate()),
                        TimeUnit.MILLISECONDS
                );
                shortLinkStats(fullShortUrl, shortLinkDO.getGid(), request, response);
                response.sendRedirect(shortLinkDO.getOriginUrl());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 统计短链接数据
     * @param fullShortUrl
     * @param gid
     * @param request
     * @param response
     */
    private void shortLinkStats(String fullShortUrl, String gid, HttpServletRequest request, HttpServletResponse response){
        if (StrUtil.isBlank(gid)){
            LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
            gid = shortLinkGotoDO.getGid();
        }
        Cookie[] cookies = request.getCookies();
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Boolean uipFirstFlag;
        //TODO 写法注意
        Date date = new Date();
        Week week = DateUtil.dayOfWeekEnum(date);
        int weekValue = week.getIso8601Value();
        int hour = DateUtil.hour(date, true);
        try {
            //统计pv，uv，uip
            AtomicReference<String> uv = new AtomicReference<>(); //TODO 用处不明白
            Runnable addResponseCookieTask = () -> {
                uv.set(UUID.fastUUID().toString());
                Cookie uvCookie = new Cookie("uv", uv.get());
                uvCookie.setMaxAge(60 * 60 * 24 * 30);
                uvCookie.setPath(fullShortUrl.substring(fullShortUrl.indexOf("/")));
                response.addCookie(uvCookie);
                uvFirstFlag.set(Boolean.TRUE);
                //存储访问过的nv cookie
                stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, uv.get());
            };
            if (ArrayUtil.isNotEmpty(cookies)){
                Arrays.stream(cookies)
                        .filter(each -> Objects.equals(each.getName(), "uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each -> {
                            uv.set(each);
                            Long uvAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                            uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                        }, addResponseCookieTask);
            } else {
                addResponseCookieTask.run();
            }
            String uip = getIp(request);
            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, uip);
            uipFirstFlag = uipAdded != null && uipAdded > 0L;
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1).uv(uvFirstFlag.get() ? 1 : 0)
                    .uip(uipFirstFlag ? 1 : 0).hour(hour)
                    .weekday(weekValue).fullShortUrl(fullShortUrl)
                    .gid(gid).date(date)
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
            //统计地区数量
            HashMap<String, Object> locateParamMap = new HashMap<>();
            locateParamMap.put("key", AMapKey);
            locateParamMap.put("ip", uip);
            String locateResultStr = HttpUtil.get(AMAP_REMOTE_URL, locateParamMap);
            JSONObject locateResultObject = JSON.parseObject(locateResultStr);
            LinkLocateStatsDO linkLocateStatsDO;
            String actualProvince;
            String actualCity;
            String infocode = locateResultObject.getString("infocode");
            if (StrUtil.isNotBlank(infocode) && StrUtil.equals(infocode, "10000")){
                String province = locateResultObject.getString("province");
                String city = locateResultObject.getString("city");
                String adcode = locateResultObject.getString("adcode");
                boolean unknownFlag = StrUtil.equals(province, "[]");
                linkLocateStatsDO = LinkLocateStatsDO.builder()
                        .date(date)
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .province(actualProvince = unknownFlag ? "未知" : province)
                        .city(actualCity = unknownFlag ? "未知" : city)
                        .adcode(unknownFlag ? "未知" : adcode)
                        .country("中国")
                        .cnt(1)
                        .build();
                linkLocateStatsMapper.shortLinkLocateStats(linkLocateStatsDO);
                //统计操作系统
                String os = getOs(request);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(date)
                        .cnt(1)
                        .os(os)
                        .build();
                linkOsStatsMapper.shortLinOsStats(linkOsStatsDO);
                //统计浏览器
                String browser = getBrowser(request);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .browser(browser)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkBrowserStatsMapper.shortLinkBrowserStats(linkBrowserStatsDO);
                //监控统计设备
                String device = getDevice(request);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .device(device)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(date)
                        .build();
                linkDeviceStatsMapper.shortLinkDeviceStats(linkDeviceStatsDO);
                //监控统计网络
                String network = getNetwork(request);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .network(network)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(date)
                        .build();
                linkNetworkStatsMapper.shortLinkNetworkStats(linkNetworkStatsDO);
                //监控访问日志
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .browser(browser)
                        .ip(uip)
                        .os(os)
                        .user(uv.get())
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .network(network)
                        .device(device)
                        .locate(StrUtil.join("-", "中国", actualProvince, actualCity))
                        .build();
                linkAccessLogsMapper.insert(linkAccessLogsDO);

                //累计短链接今日数据
                baseMapper.incrementStats(gid, fullShortUrl, 1, uvFirstFlag.get() ? 1 : 0, uipFirstFlag ? 1 : 0);
                //监控短链接今日数据
                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .date(date)
                        .todayPv(1)
                        .todayUv(uvFirstFlag.get() ? 1 : 0)
                        .todayUip(uipFirstFlag ? 1 : 0)
                        .build();
                linkStatsTodayMapper.shortLinTodayStats(linkStatsTodayDO);
            }
        } catch (Throwable ex) {
            log.error("短链接访问量统计异常", ex);
        }

    }

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO shortLinkCreateReqDTO) {
        String shortLinkSuffix = generateSuffix(shortLinkCreateReqDTO);
        String fullShortUrl = StrBuilder.create(shortLinkCreateReqDTO.getDomain())
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(shortLinkCreateReqDTO.getDomain())
                .originUrl(shortLinkCreateReqDTO.getOriginUrl())
                .gid(shortLinkCreateReqDTO.getGid())
                .createdType(shortLinkCreateReqDTO.getCreatedType())
                .validDateType(shortLinkCreateReqDTO.getValidDateType())
                .validDate(shortLinkCreateReqDTO.getValidDate())
                .describe(shortLinkCreateReqDTO.getDescribe())
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .shortUri(shortLinkSuffix)
                .enableStatus(0)
                .fullShortUrl(fullShortUrl)
                .favicon(getFavicon(shortLinkCreateReqDTO.getOriginUrl()))
                .build();
        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .gid(shortLinkDO.getGid())
                .fullShortUrl(fullShortUrl)
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        } catch (DuplicateKeyException ex){
            //TODO 误判的短链接如何处理
            //第一种，短链接确实真实存在缓存
            //第二种，短链接不一定存在缓存中
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if (hasShortLinkDO != null){
                log.warn("短链接：{} 重复入库", fullShortUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                shortLinkCreateReqDTO.getOriginUrl(),
                getLinkCacheValidTime(shortLinkCreateReqDTO.getValidDate()),
                TimeUnit.MILLISECONDS
        );
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                //测试阶段直接写死，域名管理后面会有
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(shortLinkCreateReqDTO.getOriginUrl())
                .gid(shortLinkDO.getGid())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO shortLinkUpdateReqDTO) {
        //切换分组有问题
        //因为用分组来分表，所以最好先删除
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, shortLinkUpdateReqDTO.getGid())
                .eq(ShortLinkDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null){
            throw new ClientException("短链接记录不存在");
        }
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .clickNum(hasShortLinkDO.getClickNum())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(shortLinkUpdateReqDTO.getGid())
                .originUrl(shortLinkUpdateReqDTO.getOriginUrl())
                .describe(shortLinkUpdateReqDTO.getDescribe())
                .validDate(shortLinkUpdateReqDTO.getValidDate())
                .validDateType(shortLinkUpdateReqDTO.getValidDateType())
                .build();
        if (Objects.equals(shortLinkUpdateReqDTO.getGid(), hasShortLinkDO.getGid())){
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, shortLinkUpdateReqDTO.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .set(Objects.equals(shortLinkUpdateReqDTO.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            baseMapper.update(shortLinkDO, updateWrapper);
        }else {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            baseMapper.delete(updateWrapper);
            baseMapper.insert(shortLinkDO);
        }

    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getGid, shortLinkPageReqDTO.getGid())
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(shortLinkPageReqDTO, queryWrapper);
        //TODO 这部分逻辑还是需要理一下
        return resultPage.convert(each -> {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
            result.setDomain("http://" + result.getDomain());
            return result;
        });

    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> gids) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .in("gid", gids)
                .eq("enable_status", 0)
                .groupBy("gid");
        // TODO mp的用法也得继续学，问chat？
        List<Map<String, Object>> shortLinkDoList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDoList, ShortLinkGroupCountQueryRespDTO.class);
    }

    private String generateSuffix(ShortLinkCreateReqDTO shortLinkCreateReqDTO){
        //Hash搞会有冲突的可能
        String shortUri;
        int customGenerateCount = 0;
        while (true){
            if (customGenerateCount > 0){
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            String originUrl = shortLinkCreateReqDTO.getOriginUrl();
            originUrl += System.currentTimeMillis();
            //改变原始的避免冲突 这里的处理 加毫秒值目的就是生成不同的短链接，在多线程情况下有可能进入到catch块里，
            // 因为这时还没加到布隆过滤器里面，就误判为不存在，但是数据库中其实已经插入进去了，所以在异常块里直接把完整的短链接放到布隆过滤器里面就好了
            shortUri = HashUtil.hashToBase62(originUrl);
            if (!shortUriCreateCachePenetrationBloomFilter.contains(shortLinkCreateReqDTO.getDomain() + '/' + shortUri)){
                break;
            }else {
                customGenerateCount++;
            }
            //这里可能会存在频繁访问数据库
            //加分布式锁？有性能损耗
            //查数据库意味着不支持海量并发
            //不走数据库
            //正常hash，字符串，会有大key问题或容量过大，占用内存过多
            //布隆过滤器(不存在一定不存在，存在不一定真的存在，但这个场景下
            // 存在一种情况，短链接入库成功，但是并没有添加到布隆过滤器中（可能因为进程挂掉等等原因，由于没加事务，短链接入库不会回滚）。
            // 也就是说实际上入库了，但布隆过滤器显示短链不存在，此时再次插入该短链不就越过布隆过滤器，然后被唯一索引给拦截了。
            // 因为这种情况出现的概率极低，所以把唯一索引称为兜底策略。
            //前面章节的用户名设置为唯一索引也是同样的道理。)
            // TODO 布隆过滤器的误判 要学习
        }
        return shortUri;
    }

    @SneakyThrows
    private String getFavicon(String url){
        //创建URL对象
        URL targetUrl = new URL(url);
        //打开连接
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        // 禁止自动处理重定向
        connection.setInstanceFollowRedirects(false);
        // 设置请求方法为GET
        connection.setRequestMethod("GET");
        //连接
        connection.connect();
        //获取响应码
        int responseCode = connection.getResponseCode();
        // 如果是重定向响应码
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            //获取重定向的URL
            String redirectUrl = connection.getHeaderField("Location");
            //如果重定向URL不为空
            if (redirectUrl != null) {
                // 创建新的URL对象
                URL newUrl = new URL(redirectUrl);//打开新的连接
                connection = (HttpURLConnection) newUrl.openConnection();//设置请求方法为GET
                connection.setRequestMethod("GET");//连接
                connection.connect();//获取新的响应码
                responseCode = connection.getResponseCode();
            }
        }
        // 如果响应码为200(HTTP_OK)
        if (responseCode == HttpURLConnection.HTTP_OK){
            //使用Jsoup库连接到URL并获取文档对象
            Document document = Jsoup.connect(url).get();
            //选择第一个匹配的<Link>标签，其属性包含”shortcut“或者”icon“
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            //如果存在favicon
            if (faviconLink != null){
                //返回图标绝对路径
                return faviconLink.attr("abs:href");
            }

        }
        //如果不存在图标链接，返回null
        return null;
    }
}