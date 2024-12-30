package io.cordys.listener;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主页控制器类，处理访问根页面（"/"）和登录页面（"/login"）的请求。
 * <p>
 * 该控制器负责将请求转发到 `index.html` 页面。
 * </p>
 */
@Controller
public class Index {
    /**
     * 处理根路径（"/"）的请求，并返回首页 `index.html` 页面。
     *
     * @return 返回首页的视图名称
     */
    @GetMapping("/")
    public String index() {
        return "/index.html";
    }

    /**
     * 处理移动端根路径（"/"）的请求，并返回首页 `/mobile/index.html` 页面。
     *
     * @return 返回首页的视图名称
     */
    @GetMapping("/mobile")
    public String mobileIndex() {
        return "/mobile/index.html";
    }


    /**
     * 处理登录页面（"/login"）的请求，并返回 `index.html` 页面。
     *
     * @return 返回登录页面的视图名称
     */
    @GetMapping(value = "/login")
    public String login() {
        return "/index.html";
    }
}
