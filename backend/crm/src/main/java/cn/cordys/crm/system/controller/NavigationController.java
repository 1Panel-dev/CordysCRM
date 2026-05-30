package cn.cordys.crm.system.controller;


import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.Navigation;
import cn.cordys.crm.system.dto.request.ModuleSortRequest;
import cn.cordys.crm.system.service.NavigationService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/navigation")
@Tag(name = "导航栏设置")
public class NavigationController {

    @Resource
    private NavigationService navigationService;


    @GetMapping("/list")
    @Operation(summary = "获取导航栏列表")
    public List<Navigation> getNavigationList() {
        return navigationService.getNavigationList(OrganizationContext.getOrganizationId());
    }


    @PostMapping("/sort")
    @Operation(summary = "导航栏排序")
    public void sortModule(@Validated @RequestBody ModuleSortRequest request) {
        navigationService.sort(request, SessionUtils.getUserId());
    }

}
