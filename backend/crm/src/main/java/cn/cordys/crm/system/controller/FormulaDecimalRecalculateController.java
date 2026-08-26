package cn.cordys.crm.system.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.service.FormulaDecimalRecalculateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计算字段历史数据重算。
 */
@RestController
@RequestMapping("/module/form/formula/decimal")
@Tag(name = "模块-表单设置-计算字段重算")
public class FormulaDecimalRecalculateController {

    @Resource
    private FormulaDecimalRecalculateService formulaDecimalRecalculateService;

    @GetMapping("/recalculate")
    @Operation(summary = "后台重算订单和合同计算字段小数位")
    public FormulaDecimalRecalculateService.RecalculateTask recalculate(@RequestParam(required = false) String orgId) {
        String targetOrgId = orgId == null ? OrganizationContext.getOrganizationId() : orgId;
        return formulaDecimalRecalculateService.startOrGetRecalculateTask(targetOrgId);
    }
}
