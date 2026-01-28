package cn.cordys.crm.integration.sqlbot.handler;

import cn.cordys.common.constants.FormKey;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

@Component
public class OpportunityPermissionHandler extends DataScopeTablePermissionHandler {

  @Resource private ModuleFormCacheService moduleFormCacheService;

  @PostConstruct
  public void registerHandler() {
    TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.OPPORTUNITY, this);
  }

  @Override
  public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
    ModuleFormConfigDTO formConfig =
        moduleFormCacheService.getBusinessFormConfig(
            FormKey.OPPORTUNITY.getKey(), OrganizationContext.getOrganizationId());
    List<FieldDTO> filterFields =
        filterSystemFields(
            table.getFields(), Set.of("organization_id", "last_stage", "status", "failure_reason"));

    filterFields.add(new FieldDTO("varchar(255)", "customer_name", "客户名称"));
    filterFields.add(new FieldDTO("varchar(255)", "contact_name", "联系人名称"));

    table.setFields(filterFields);

    super.handleTable(table, tableHandleParam, formConfig);
  }

  @Override
  protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
    String fieldName = sqlBotField.getName();
    if (Strings.CS.equals(fieldName, "stage")) {
      return getStageFieldSql();
    } else if (Strings.CS.equals(fieldName, "customer_name")) {
      return getCustomerNameFieldSql();
    } else if (Strings.CS.equals(fieldName, "contact_name")) {
      return getContactNameFieldSql();
    } else if (Strings.CS.equals(fieldName, "products")) {
      return getProductsFieldSql();
    } else {
      return getDefaultFieldSql(sqlBotField);
    }
  }

  protected String getStageFieldSql() {
    return "(select osc.name from opportunity_stage_config osc where c.stage = osc.id limit 1) as"
               + " stage";
  }

  protected String getCustomerNameFieldSql() {
    return "(select customer.name from customer where c.customer_id = customer.id limit 1) as"
               + " customer_name";
  }

  protected String getContactNameFieldSql() {
    return "(select customer_contact.name from customer_contact where c.contact_id ="
               + " customer_contact.id limit 1) as contact_name";
  }
}
