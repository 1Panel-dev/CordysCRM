package io.cordys.crm.integration.sqlbot.handler;


import io.cordys.common.constants.FormKey;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.opportunity.constants.StageType;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.integration.sqlbot.dto.TableDTO;
import io.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class OpportunityPermissionHandler extends DataScopeTablePermissionHandler {

    {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.OPPORTUNITY, this);
    }

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), OrganizationContext.getOrganizationId());
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(),
                Set.of("organization_id", "last_stage", "status", "failure_reason"));
        table.setFields(filterFields);

        super.handleTable(table, tableHandleParam, formConfig);
    }

    @Override
    protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
        String fieldName = sqlBotField.getName();
        if (StringUtils.equals(fieldName, "stage")) {
            return getSystemOptionFileSql(Arrays.stream(StageType.values()),
                    StageType::name,
                    (stageType) ->
                            switch (stageType) {
                                case CREATE -> "新建";
                                case CLEAR_REQUIREMENTS -> "需求明确";
                                case SCHEME_VALIDATION -> "方案验证";
                                case PROJECT_PROPOSAL_REPORT -> "项目汇报";
                                case BUSINESS_PROCUREMENT -> "商务采购";
                                case SUCCESS -> "成功";
                                case FAIL -> "失败";
                            },
                    fieldName);
        } else if (StringUtils.equals(fieldName, "customer_id")) {
            sqlBotField.setName("customer_name");
            sqlBotField.setComment("客户名称");
            return getCustomerFieldSql();
        } else if (StringUtils.equals(fieldName, "contact_id")) {
            sqlBotField.setName("contact_name");
            sqlBotField.setComment("联系人名称");
            return getContactFieldSql();
        } else {
            return getDefaultFieldSql(sqlBotField);
        }
    }

    protected String getCustomerFieldSql() {
        return "(select customer.name from customer where c.customer_id = customer.id limit 1) as customer_name";
    }

    protected String getContactFieldSql() {
        return "(select customer_contact.name from customer_contact where c.contact_id = customer_contact.id limit 1) as contact_name";
    }
}
