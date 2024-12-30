package io.cordys.crm.system.controller;

import io.cordys.crm.system.domain.SysUser;
import io.cordys.crm.system.service.DemoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主页控制器类，处理访问根页面（"/"）和登录页面（"/login"）的请求。
 * <p>
 * 该控制器负责将请求转发到 `index.html` 页面。
 * </p>
 */
@Controller
public class IndexController {
    @Resource
    private DemoService cordysService;

    /**
     * 处理根路径（"/"）的请求，并返回首页 `index.html` 页面。
     *
     * @return 返回首页的视图名称
     */
    @GetMapping("/")
    public String index() {
        return "/web/index.html";
    }

    /**
     * 处理登录页面（"/login"）的请求，并返回 `index.html` 页面。
     *
     * @return 返回登录页面的视图名称
     */
    @GetMapping(value = "/login")
    public String login() {
        return "/web/index.html";
    }

    // todo: 以下部分 DEMO 日志记录功能示例，后续删除

    @GetMapping(value = "/cordys/add")
    public SysUser add() {
        SysUser sysUser = new SysUser();
        sysUser.setId("1");
        sysUser.setName("test");

        cordysService.addUser(sysUser);

        return sysUser;
    }

    @GetMapping(value = "/cordys/delete")
    public void del() {
        cordysService.deleteUser("testUser");
    }

    @GetMapping(value = "/cordys/update")
    public SysUser update() {
        SysUser sysUser = new SysUser();
        sysUser.setId("1");
        sysUser.setName("test");
        cordysService.updateUser(sysUser);
        return sysUser;
    }

}
