package cn.cordys.crm.follow.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "跟进记录统一页面")
@RestController
@RequestMapping("/follow/record")
public class FollowUpRecordController {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;


    @GetMapping("/module/form")
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_RECORD.getKey(), OrganizationContext.getOrganizationId());
    }

}
