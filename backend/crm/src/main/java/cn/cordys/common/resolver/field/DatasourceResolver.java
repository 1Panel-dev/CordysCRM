package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.system.domain.Product;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.service.ProductService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;

public class DatasourceResolver extends AbstractModuleFieldResolver<DatasourceField> {

	private static final CustomerService customerService;
	private static final OpportunityService opportunityService;
	private static final ClueService clueService;
	private static final CustomerContactService contactService;
	private static final ProductService productService;

	static {
		customerService = CommonBeanFactory.getBean(CustomerService.class);
		opportunityService = CommonBeanFactory.getBean(OpportunityService.class);
		clueService = CommonBeanFactory.getBean(ClueService.class);
		contactService = CommonBeanFactory.getBean(CustomerContactService.class);
		productService = CommonBeanFactory.getBean(ProductService.class);
	}

	@Override
	public void validate(DatasourceField customField, Object value) {

	}


	@Override
	public Object trans2Value(DatasourceField datasourceField, String value) {
		if (StringUtils.isBlank(value)) {
			return StringUtils.EMPTY;
		}

		if (Strings.CI.equals(datasourceField.getDataSourceType(), "CUSTOMER")) {
			return customerService.getCustomerName(value);
		}

		if(Strings.CI.equals(datasourceField.getDataSourceType(), "OPPORTUNITY")) {
			return opportunityService.getOpportunityName(value);
		}

		if (Strings.CI.equals(datasourceField.getDataSourceType(), "CLUE")) {
			return clueService.getClueName(value);
		}

		return StringUtils.EMPTY;
	}

	@Override
	public Object text2Value(DatasourceField field, String text) {
		if (StringUtils.isBlank(text)) {
			return StringUtils.EMPTY;
		}
		if (Strings.CI.equals(field.getDataSourceType(), "CUSTOMER")) {
			List<Customer> customerList = customerService.getCustomerListByNames(List.of(text));
			return CollectionUtils.isEmpty(customerList) ? text : customerList.getFirst().getId();
		}
		if(Strings.CI.equals(field.getDataSourceType(), "OPPORTUNITY")) {
			List<Opportunity> opportunityList = opportunityService.getOpportunityListByNames(List.of(text));
			return CollectionUtils.isEmpty(opportunityList) ? text : opportunityList.getFirst().getId();
		}
		if (Strings.CI.equals(field.getDataSourceType(), "CLUE")) {
			List<Clue> clueList = clueService.getClueListByNames(List.of(text));
			return CollectionUtils.isEmpty(clueList) ? text : clueList.getFirst().getId();
		}
		if (Strings.CI.equals(field.getDataSourceType(), "CONTACT")) {
			List<CustomerContact> contactList = contactService.getContactListByNames(List.of(text));
			return CollectionUtils.isEmpty(contactList) ? text : contactList.getFirst().getId();
		}
		if (Strings.CI.equals(field.getDataSourceType(), "PRODUCT")) {
			List<Product> productList = productService.getProductListByNames(List.of(text));
			return CollectionUtils.isEmpty(productList) ? text : productList.getFirst().getId();
		}
		return text;
	}
}
