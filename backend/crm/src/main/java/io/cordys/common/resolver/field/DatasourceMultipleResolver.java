package io.cordys.common.resolver.field;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.crm.clue.service.ClueService;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.opportunity.service.OpportunityService;
import io.cordys.crm.system.dto.field.DatasourceMultipleField;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class DatasourceMultipleResolver extends AbstractModuleFieldResolver<DatasourceMultipleField> {

    private static final CustomerService customerService;
    private static final OpportunityService opportunityService;
    private static final ClueService clueService;

    static {
        customerService = CommonBeanFactory.getBean(CustomerService.class);
        opportunityService = CommonBeanFactory.getBean(OpportunityService.class);
        clueService = CommonBeanFactory.getBean(ClueService.class);
    }

    @Override
    public void validate(DatasourceMultipleField customField, Object value) {

    }

    @Override
    public Object parse2Value(DatasourceMultipleField customField, String value) {
        return parse2Array(value);
    }

    @Override
    public String parse2String(DatasourceMultipleField customField, Object value) {
        return getJsonString(value);
    }


    @Override
    public Object trans2Value(DatasourceMultipleField datasourceMultipleField, String value) {
        if (StringUtils.isBlank(value) || StringUtils.equals(value, "[]")) {
            return StringUtils.EMPTY;
        }
        List list = JSON.parseArray(value,String.class);

        if (StringUtils.equalsIgnoreCase(datasourceMultipleField.getDataSourceType(), "CUSTOMER")) {
            return customerService.getCustomerNameByIds(list);
        }

        if (StringUtils.equalsIgnoreCase(datasourceMultipleField.getDataSourceType(), "OPPORTUNITY")) {
            return opportunityService.getOpportunityNameByIds(list);
        }

        if (StringUtils.equalsIgnoreCase(datasourceMultipleField.getDataSourceType(), "CLUE")) {
            return clueService.getClueNameByIds(list);
        }

        return StringUtils.EMPTY;
    }
}
