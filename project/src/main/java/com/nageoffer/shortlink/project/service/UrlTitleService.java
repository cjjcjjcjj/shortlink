package com.nageoffer.shortlink.project.service;


/**
 * URL标题接口层
 */
public interface UrlTitleService {


    /**
     * 根据URL获取标题
     * @param url 目标网站路径
     * @return 标题
     */
    String getTitleByUrl(String url);
}
