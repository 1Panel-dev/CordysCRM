package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.domain.BusinessTitle;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.domain.ContractPaymentRecord;
import cn.cordys.crm.contract.service.BusinessTitleService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.field.DatasourceMultipleField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Objects;

public class DatasourceMultipleResolver extends AbstractModuleFieldResolver<DatasourceMultipleField> {

    private static final CustomerService customerService;
    private static final OpportunityService opportunityService;
    private static final ClueService clueService;
    private static final CustomerContactService contactService;
    private static final ProductService productService;
    private static final ProductPriceService productPriceService;
    private static final OpportunityQuotationService opportunityQuotationService;
	private static final ContractPaymentPlanService contractPaymentPlanService;
	private static final ContractPaymentRecordService contractPaymentRecordService;
	private static final BusinessTitleService businessTitleService;

	public static final String EMPTY_ARRAY_STRING = "[]";

    static {
        customerService = CommonBeanFactory.getBean(CustomerService.class);
        opportunityService = CommonBeanFactory.getBean(OpportunityService.class);
        clueService = CommonBeanFactory.getBean(ClueService.class);
        contactService = CommonBeanFactory.getBean(CustomerContactService.class);
        productService = CommonBeanFactory.getBean(ProductService.class);
        productPriceService = CommonBeanFactory.getBean(ProductPriceService.class);
        opportunityQuotationService = CommonBeanFactory.getBean(OpportunityQuotationService.class);
		contractPaymentRecordService = CommonBeanFactory.getBean(ContractPaymentRecordService.class);
		contractPaymentPlanService = CommonBeanFactory.getBean(ContractPaymentPlanService.class);
		businessTitleService = CommonBeanFactory.getBean(BusinessTitleService.class);
    }

    @Override
    public void validate(DatasourceMultipleField customField, Object value) {

    }

    @Override
    public Object convertToValue(DatasourceMultipleField customField, String value) {
        return parse2Array(value);
    }

    @Override
    public String convertToString(DatasourceMultipleField customField, Object value) {
        return getJsonString(value);
    }


    @Override
    public Object transformToValue(DatasourceMultipleField datasourceMultipleField, String value) {
        if (StringUtils.isBlank(value) || Strings.CS.equals(value, EMPTY_ARRAY_STRING)) {
            return StringUtils.EMPTY;
        }
        var list = JSON.parseArray(value, String.class);

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.CUSTOMER.name())) {
            return Objects.requireNonNull(customerService).getCustomerNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.CONTACT.name())) {
            return Objects.requireNonNull(contactService).getContactNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.OPPORTUNITY.name())) {
            return Objects.requireNonNull(opportunityService).getOpportunityNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.CLUE.name())) {
            return Objects.requireNonNull(clueService).getClueNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.PRODUCT.name())) {
            return Objects.requireNonNull(productService).getProductNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.PRICE.name())) {
            return Objects.requireNonNull(productPriceService).getProductPriceNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.QUOTATION.name())) {
            return Objects.requireNonNull(opportunityQuotationService).getQuotationNameByIds(list);
        }

		if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.CONTRACT_PAYMENT_RECORD.name())) {
			return Objects.requireNonNull(contractPaymentRecordService).getRecordNameByIds(list);
		}

		if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.PAYMENT_PLAN.name())) {
			return Objects.requireNonNull(contractPaymentPlanService).getPlanNameByIds(list);
		}

		if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.BUSINESS_TITLE.name())) {
			return Objects.requireNonNull(businessTitleService).getTitleNameByIds(list);
		}

        return StringUtils.EMPTY;
    }

    @Override
    public Object textToValue(DatasourceMultipleField field, String text) {
        if (StringUtils.isBlank(text) || Strings.CS.equals(text, EMPTY_ARRAY_STRING)) {
            return StringUtils.EMPTY;
        }
        List<String> names = parseFakeJsonArray(text);

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CUSTOMER.name())) {
            List<Customer> customerList = Objects.requireNonNull(customerService).getCustomerListByNames(names);
            List<String> ids = customerList.stream().map(Customer::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.OPPORTUNITY.name())) {
            List<Opportunity> opportunityList = Objects.requireNonNull(opportunityService).getOpportunityListByNames(names);
            List<String> ids = opportunityList.stream().map(Opportunity::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CLUE.name())) {
            List<Clue> clueList = Objects.requireNonNull(clueService).getClueListByNames(names);
            List<String> ids = clueList.stream().map(Clue::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTACT.name())) {
            List<CustomerContact> contactList = Objects.requireNonNull(contactService).getContactListByNames(names);
            List<String> ids = contactList.stream().map(CustomerContact::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PRODUCT.name())) {
            List<Product> productList = Objects.requireNonNull(productService).getProductListByNames(names);
            List<String> ids = productList.stream().map(Product::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PRICE.name())) {
			List<ProductPrice> prices = Objects.requireNonNull(productPriceService).getProductPriceListByNames(names);
			List<String> ids = prices.stream().map(ProductPrice::getId).toList();
			return CollectionUtils.isEmpty(ids) ? names : ids;
		}

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.QUOTATION.name())) {
			List<OpportunityQuotation> quotations = Objects.requireNonNull(opportunityQuotationService).getQuotationListByNames(names);
			List<String> ids = quotations.stream().map(OpportunityQuotation::getId).toList();
			return CollectionUtils.isEmpty(ids) ? names : ids;
		}

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTRACT_PAYMENT_RECORD.name())) {
			List<ContractPaymentRecord> records = Objects.requireNonNull(contractPaymentRecordService).getRecordListByNames(names);
			List<String> ids = records.stream().map(ContractPaymentRecord::getId).toList();
			return CollectionUtils.isEmpty(ids) ? names : ids;
		}

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PAYMENT_PLAN.name())) {
			List<ContractPaymentPlan> plans = Objects.requireNonNull(contractPaymentPlanService).getPlanListByNames(names);
			List<String> ids = plans.stream().map(ContractPaymentPlan::getId).toList();
			return CollectionUtils.isEmpty(ids) ? names : ids;
		}

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.BUSINESS_TITLE.name())) {
			List<BusinessTitle> titles = Objects.requireNonNull(businessTitleService).getBusinessTitleListByNames(names);
			List<String> ids = titles.stream().map(BusinessTitle::getId).toList();
			return CollectionUtils.isEmpty(ids) ? names : ids;
		}
        return names;
    }
}
