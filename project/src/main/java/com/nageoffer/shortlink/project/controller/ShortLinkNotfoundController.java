package com.nageoffer.shortlink.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 短链接不存在跳转控制器
 */
@Controller
public class ShortLinkNotfoundController {
    //RestController会返回json形式，这个Controller会先返回和视图匹配的

    /**
     * 短链接不存在跳转页面
     * @return
     */
    @RequestMapping("/page/notfound")
    public String notfound() {
        return "notfound";
    }
}
