package io.cordys.crm.integration.sqlbot.controller;

import io.cordys.common.response.handler.NoResultHolder;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.context.OrganizationContext;
import io.cordys.security.SessionUtils;
import io.cordys.crm.integration.sqlbot.dto.SQLBotDTO;
import io.cordys.crm.integration.sqlbot.service.DataSourceService;
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
        SQLBotDTO databaseSchema = dataSourceService.getDatabaseSchema(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        LogUtils.info("当前用户：{} : {} 的数据结构：{} ", SessionUtils.getUser().getName(), SessionUtils.getUserId(), JSON.toFormatJSONString(databaseSchema));
        return databaseSchema;
    }
}
