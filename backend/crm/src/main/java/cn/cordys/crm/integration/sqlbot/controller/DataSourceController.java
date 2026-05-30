package cn.cordys.crm.integration.sqlbot.controller;

import cn.cordys.common.response.handler.NoResultHolder;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.sqlbot.dto.SQLBotDTO;
import cn.cordys.crm.integration.sqlbot.service.DataSourceService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class DataSourceController {

    @Resource
    private DataSourceService dataSourceService;

    /**
     * 获取数据库结构。
     * <p>
     * 该方法用于获取当前系统的数据库结构信息。
     * </p>
     * 这个 API 风险较高，可能会泄露数据库结构信息，因此需要谨慎使用。
     *
     * @return 数据库结构信息。
     */
    @GetMapping("/db/structure")
    @Operation(summary = "获取数据库结构")
    @NoResultHolder
    public SQLBotDTO getDBSchema() {
        return dataSourceService.getDatabaseSchema(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
