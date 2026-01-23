package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.domain.BusinessTitle;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.domain.ContractPaymentRecord;
import cn.cordys.crm.contract.service.BusinessTitleService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.contract.service.ContractService;
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
import cn.cordys.crm.system.dto.field.DatasourceField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Objects;

public class DatasourceResolver extends AbstractModuleFieldResolver<DatasourceField> {

    private static final CustomerService customerService;
    private static final OpportunityService opportunityService;
    private static final ClueService clueService;
    private static final CustomerContactService contactService;
    private static final ProductService productService;
    private static final ProductPriceService productPriceService;
    private static final OpportunityQuotationService opportunityQuotationService;
	private static final ContractService contractService;
	private static final ContractPaymentPlanService contractPaymentPlanService;
	private static final ContractPaymentRecordService contractPaymentRecordService;
	private static final BusinessTitleService businessTitleService;

    static {
        customerService = CommonBeanFactory.getBean(CustomerService.class);
        opportunityService = CommonBeanFactory.getBean(OpportunityService.class);
        clueService = CommonBeanFactory.getBean(ClueService.class);
        contactService = CommonBeanFactory.getBean(CustomerContactService.class);
        productService = CommonBeanFactory.getBean(ProductService.class);
        productPriceService = CommonBeanFactory.getBean(ProductPriceService.class);
        opportunityQuotationService = CommonBeanFactory.getBean(OpportunityQuotationService.class);
		contractService = CommonBeanFactory.getBean(ContractService.class);
		contractPaymentRecordService = CommonBeanFactory.getBean(ContractPaymentRecordService.class);
		contractPaymentPlanService = CommonBeanFactory.getBean(ContractPaymentPlanService.class);
		businessTitleService = CommonBeanFactory.getBean(BusinessTitleService.class);
    }

    @Override
    public void validate(DatasourceField customField, Object value) {

    }


    @Override
    public Object transformToValue(DatasourceField datasourceField, String value) {
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.CUSTOMER.name())) {
            return Objects.requireNonNull(customerService).getCustomerName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.CONTACT.name())) {
            return Objects.requireNonNull(contactService).getContactName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.OPPORTUNITY.name())) {
            return Objects.requireNonNull(opportunityService).getOpportunityName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.CLUE.name())) {
            return Objects.requireNonNull(clueService).getClueName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.PRODUCT.name())) {
            return Objects.requireNonNull(productService).getProductName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.PRICE.name())) {
            return Objects.requireNonNull(productPriceService).getProductPriceName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.QUOTATION.name())) {
            return Objects.requireNonNull(opportunityQuotationService).getQuotationName(value);
        }

		if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.PAYMENT_PLAN.name())) {
			return Objects.requireNonNull(contractPaymentPlanService).getPlanName(value);
		}

		if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.CONTRACT_PAYMENT_RECORD.name())) {
			return Objects.requireNonNull(contractPaymentRecordService).getRecordNameById(value);
		}

		if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.BUSINESS_TITLE.name())) {
			return Objects.requireNonNull(businessTitleService).getBusinessTitleName(value);
		}

        return StringUtils.EMPTY;
    }

    @Override
    public Object textToValue(DatasourceField field, String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CUSTOMER.name())) {
            List<Customer> customerList = Objects.requireNonNull(customerService).getCustomerListByNames(List.of(text));
            return CollectionUtils.isEmpty(customerList) ? text : customerList.getFirst().getId();
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.OPPORTUNITY.name())) {
            List<Opportunity> opportunityList = Objects.requireNonNull(opportunityService).getOpportunityListByNames(List.of(text));
            return CollectionUtils.isEmpty(opportunityList) ? text : opportunityList.getFirst().getId();
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CLUE.name())) {
            List<Clue> clueList = Objects.requireNonNull(clueService).getClueListByNames(List.of(text));
            return CollectionUtils.isEmpty(clueList) ? text : clueList.getFirst().getId();
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTACT.name())) {
            List<CustomerContact> contactList = Objects.requireNonNull(contactService).getContactListByNames(List.of(text));
            return CollectionUtils.isEmpty(contactList) ? text : contactList.getFirst().getId();
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PRODUCT.name())) {
            List<Product> productList = Objects.requireNonNull(productService).getProductListByNames(List.of(text));
            return CollectionUtils.isEmpty(productList) ? text : productList.getFirst().getId();
        }
		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PRICE.name())) {
			List<ProductPrice> productPrices = Objects.requireNonNull(productPriceService).getProductPriceListByNames(List.of(text));
			return CollectionUtils.isEmpty(productPrices) ? text : productPrices.getFirst().getId();
		}
		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.QUOTATION.name())) {
			List<OpportunityQuotation> quotations = Objects.requireNonNull(opportunityQuotationService).getQuotationListByNames(List.of(text));
			return CollectionUtils.isEmpty(quotations) ? text : quotations.getFirst().getId();
		}
		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTRACT.name())) {
			List<Contract> contracts = Objects.requireNonNull(contractService).getContractListByNames(List.of(text));
			return CollectionUtils.isEmpty(contracts) ? text : contracts.getFirst().getId();
		}
		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PAYMENT_PLAN.name())) {
			List<ContractPaymentPlan> plans = Objects.requireNonNull(contractPaymentPlanService).getPlanListByNames(List.of(text));
			return CollectionUtils.isEmpty(plans) ? text : plans.getFirst().getId();
		}
		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTRACT_PAYMENT_RECORD.name())) {
			List<ContractPaymentRecord> records = Objects.requireNonNull(contractPaymentRecordService).getRecordListByNames(List.of(text));
			return CollectionUtils.isEmpty(records) ? text : records.getFirst().getId();
		}
		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.BUSINESS_TITLE.name())) {
			List<BusinessTitle> businessTitles = Objects.requireNonNull(businessTitleService).getBusinessTitleListByNames(List.of(text));
			return CollectionUtils.isEmpty(businessTitles) ? text : businessTitles.getFirst().getId();
		}
        return text;
    }
}
