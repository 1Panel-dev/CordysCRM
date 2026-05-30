package cn.cordys.crm.search.controller;


import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.search.request.FieldMaskConfigDTO;
import cn.cordys.crm.search.service.SearchFieldMaskConfigService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mask/config")
@Tag(name = "脱敏字段配置")
public class SearchFieldMaskConfigController {

    @Resource
    private SearchFieldMaskConfigService searchFieldMaskConfigService;


    @PostMapping("/save")
    @Operation(summary = "保存配置")
    public void save(@Validated @RequestBody FieldMaskConfigDTO request) {
        searchFieldMaskConfigService.save(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/get")
    @Operation(summary = "获取脱敏字段配置")
    public FieldMaskConfigDTO get() {
        return searchFieldMaskConfigService.get(OrganizationContext.getOrganizationId());
    }
}
