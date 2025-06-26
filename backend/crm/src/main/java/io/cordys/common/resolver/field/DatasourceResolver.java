package io.cordys.common.resolver.field;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.clue.service.ClueService;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.opportunity.service.OpportunityService;
import io.cordys.crm.system.dto.field.DatasourceField;
import org.apache.commons.lang3.StringUtils;

public class DatasourceResolver extends AbstractModuleFieldResolver<DatasourceField> {

	private static final CustomerService customerService;
	private static final OpportunityService opportunityService;
	private static final ClueService clueService;

	static {
		customerService = CommonBeanFactory.getBean(CustomerService.class);
		opportunityService = CommonBeanFactory.getBean(OpportunityService.class);
		clueService = CommonBeanFactory.getBean(ClueService.class);
	}

	@Override
	public void validate(DatasourceField customField, Object value) {

	}


	@Override
	public Object trans2Value(DatasourceField datasourceField, String value) {
		if (StringUtils.isBlank(value)) {
			return StringUtils.EMPTY;
		}

		if (StringUtils.equalsIgnoreCase(datasourceField.getDataSourceType(), "CUSTOMER")) {
			return customerService.getCustomerName(value);
		}

		if(StringUtils.equalsIgnoreCase(datasourceField.getDataSourceType(), "OPPORTUNITY")) {
			return opportunityService.getOpportunityName(value);
		}

		if (StringUtils.equalsIgnoreCase(datasourceField.getDataSourceType(), "CLUE")) {
			return clueService.getClueName(value);
		}

		return StringUtils.EMPTY;
	}
}
