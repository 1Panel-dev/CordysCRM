package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.crm.system.dto.request.UserPageRequest;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.crm.system.service.OrganizationUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/user")
@Tag(name = "用户(员工)")
public class OrganizationUserController {

    @Resource
    private OrganizationUserService organizationUserService;


    @PostMapping("/list")
    @Operation(summary = "用户(员工)-列表查询")
    @RequiresPermissions(PermissionConstants.OPERATION_LOG_READ)
    public Pager<List<UserPageResponse>> list(@Validated @RequestBody UserPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
        return PageUtils.setPageInfo(page, organizationUserService.list(request));
    }

}
