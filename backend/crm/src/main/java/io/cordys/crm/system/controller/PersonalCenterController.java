package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.service.OrganizationUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/personal/center")
@Tag(name = "个人中心")
public class PersonalCenterController {

    @Resource
    private OrganizationUserService organizationUserService;

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
        return organizationUserService.getUserDetail(id);
    }

}
