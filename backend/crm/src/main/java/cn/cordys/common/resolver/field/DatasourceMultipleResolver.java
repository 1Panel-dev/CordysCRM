package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.domain.*;
import cn.cordys.crm.contract.service.*;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.order.service.OrderService;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
	private static final OrderService orderService;
	private static final ContractService contractService;
	private static final CustomFormDataService customFormDataService;
    private static final ContractInvoiceService invoiceService;

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
        orderService = CommonBeanFactory.getBean(OrderService.class);
		contractService = CommonBeanFactory.getBean(ContractService.class);
        customFormDataService = CommonBeanFactory.getBean(CustomFormDataService.class);
        invoiceService =  CommonBeanFactory.getBean(ContractInvoiceService.class);
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
        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.ORDER.name())) {
            return Objects.requireNonNull(orderService).getOrderNameByIds(list);
        }
		if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.CONTRACT.name())) {
			return Objects.requireNonNull(contractService).getContractNameByIds(list);
		}
        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), FieldSourceType.INVOICE.name())) {
            return Objects.requireNonNull(invoiceService).getInvoiceNameByIds(list);
        }

        return Objects.requireNonNull(customFormDataService).getNameStrByIds(list);
    }

    @Override
    public Object textToValue(DatasourceMultipleField field, String text) {
        if (StringUtils.isBlank(text) || Strings.CS.equals(text, EMPTY_ARRAY_STRING)) {
            return StringUtils.EMPTY;
        }
        List<String> names = parseFakeJsonArray(text);

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CUSTOMER.name())) {
            List<Customer> customerList = Objects.requireNonNull(customerService).getCustomerListByNames(names);
            if(CollectionUtils.isEmpty(customerList)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = customerList.stream().collect(Collectors.toMap(Customer::getName, Customer::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.OPPORTUNITY.name())) {
            List<Opportunity> opportunityList = Objects.requireNonNull(opportunityService).getOpportunityListByNames(names);
            if (CollectionUtils.isEmpty(opportunityList)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = opportunityList.stream().collect(Collectors.toMap(Opportunity::getName, Opportunity::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CLUE.name())) {
            List<Clue> clueList = Objects.requireNonNull(clueService).getClueListByNames(names);
            if(CollectionUtils.isEmpty(clueList)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = clueList.stream().collect(Collectors.toMap(Clue::getName, Clue::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTACT.name())) {
            List<CustomerContact> contactList = Objects.requireNonNull(contactService).getContactListByNames(names);
            if(CollectionUtils.isEmpty(contactList)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = contactList.stream().collect(Collectors.toMap(CustomerContact::getName, CustomerContact::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PRODUCT.name())) {
            List<Product> productList = Objects.requireNonNull(productService).getProductListByNames(names);
            if(CollectionUtils.isEmpty(productList)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = productList.stream().collect(Collectors.toMap(Product::getName, Product::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
        }

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PRICE.name())) {
			List<ProductPrice> prices = Objects.requireNonNull(productPriceService).getProductPriceListByNames(names);
            if(CollectionUtils.isEmpty(prices)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = prices.stream().collect(Collectors.toMap(ProductPrice::getName, ProductPrice::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
		}

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.QUOTATION.name())) {
			List<OpportunityQuotation> quotations = Objects.requireNonNull(opportunityQuotationService).getQuotationListByNames(names);
            if(CollectionUtils.isEmpty(quotations)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = quotations.stream().collect(Collectors.toMap(OpportunityQuotation::getName, OpportunityQuotation::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
		}

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTRACT_PAYMENT_RECORD.name())) {
			List<ContractPaymentRecord> records = Objects.requireNonNull(contractPaymentRecordService).getRecordListByNames(names);
            if(CollectionUtils.isEmpty(records)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = records.stream().collect(Collectors.toMap(ContractPaymentRecord::getName, ContractPaymentRecord::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
		}

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PAYMENT_PLAN.name())) {
			List<ContractPaymentPlan> plans = Objects.requireNonNull(contractPaymentPlanService).getPlanListByNames(names);
            if(CollectionUtils.isEmpty(plans)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = plans.stream().collect(Collectors.toMap(ContractPaymentPlan::getName, ContractPaymentPlan::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
		}

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.BUSINESS_TITLE.name())) {
			List<BusinessTitle> titles = Objects.requireNonNull(businessTitleService).getBusinessTitleListByNames(names);
            if(CollectionUtils.isEmpty(titles)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = titles.stream().collect(Collectors.toMap(BusinessTitle::getName, BusinessTitle::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
		}

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.ORDER.name())) {
            List<Order> orders = Objects.requireNonNull(orderService).getOrderListByNames(names);
            if(CollectionUtils.isEmpty(orders)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = orders.stream().collect(Collectors.toMap(Order::getName, Order::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
        }

		if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTRACT.name())) {
			List<Contract> contracts = Objects.requireNonNull(contractService).getContractListByNames(names);
            if(CollectionUtils.isEmpty(contracts)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = contracts.stream().collect(Collectors.toMap(Contract::getName, Contract::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
		}

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.INVOICE.name())) {
            List<ContractInvoice> invoices = Objects.requireNonNull(invoiceService).getContractInvoiceListByNames(names);
            if(CollectionUtils.isEmpty(invoices)) {
                return StringUtils.EMPTY;
            }
            Map<String, String> nameMaps = invoices.stream().collect(Collectors.toMap(ContractInvoice::getName, ContractInvoice::getId));
            return names.stream()
                    .filter(name -> name != null && nameMaps.containsKey(name))
                    .map(nameMaps::get)
                    .distinct()
                    .toList();
        }

        List<CustomFormData> customForms = Objects.requireNonNull(customFormDataService).selectByNames(names);
        if(CollectionUtils.isEmpty(customForms)) {
            return StringUtils.EMPTY;
        }
        Map<String, String> nameMaps = customForms.stream().collect(Collectors.toMap(CustomFormData::getName, CustomFormData::getId));
        return names.stream()
                .filter(name -> name != null && nameMaps.containsKey(name))
                .map(nameMaps::get)
                .distinct()
                .toList();
    }
}
