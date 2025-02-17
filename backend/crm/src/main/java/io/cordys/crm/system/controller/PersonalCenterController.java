package io.cordys.crm.system.controller;

import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.service.PersonalCenterService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/personal/center")
@Tag(name = "个人中心")
public class PersonalCenterController {

    @Resource
    private PersonalCenterService personalCenterService;

    //接口一
    /**
     * 根据时间查询当前登录销售的线索，商机，客户跟进计划；
     *  根据计划日期，创建计划并有相应跟进记录的，显示绿点， 状态一
     *  有计划没有跟进记录的显示红点； 状态二
     *  如果是当天，有计划但没有跟进记录的显示黄点； 状态三
     */


//接口二
    /**
     *重复客户查询
     * 重复 ： 展示重复的线索和商机
     * 不重复 ：直接返回
     */

    @GetMapping("/info/{id}")
    @Operation(summary = "当前用户详情")
    public UserResponse getUserDetail(@PathVariable String id) {
        return personalCenterService.getUserDetail(id);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/mail/send_code")
    public void sendCode(@RequestParam(value = "email") String email) {
        personalCenterService.sendCode(email, OrganizationContext.getOrganizationId());
    }

    /**
     * 校验验证码
     */
    @PostMapping("/verifyCode")
    public String verifyCode(@RequestParam(value = "email") String email, @RequestParam(value = "code") String code) {
        return personalCenterService.verifyCode(email, code);
    }

    /**
     * 更新密码
     */
    @PostMapping("/info/reset")
    @Operation(summary = "用户密码重置")
    public void resetUserPassword(@RequestParam(value = "password") String password) {
        personalCenterService.resetUserPassword(password, SessionUtils.getUserId());
    }
}
