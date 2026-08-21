package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
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
    private static final OrderService orderService;
    private static final CustomFormDataService customFormDataService;
    private static final ContractInvoiceService invoiceService;

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
        orderService = CommonBeanFactory.getBean(OrderService.class);
        customFormDataService = CommonBeanFactory.getBean(CustomFormDataService.class);
        invoiceService = CommonBeanFactory.getBean(ContractInvoiceService.class);
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
        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.ORDER.name())) {
            return Objects.requireNonNull(orderService).getOrderName(value);
        }
        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.CONTRACT.name())) {
            return Objects.requireNonNull(contractService).getContractName(value);
        }
        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.INVOICE.name())) {
            return Objects.requireNonNull(invoiceService).getInvoiceName(value);
        }

        return Objects.requireNonNull(customFormDataService).getNameById(value);
    }

    @Override
    public Object textToValue(DatasourceField field, String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CUSTOMER.name())) {
            List<Customer> customerList = Objects.requireNonNull(customerService).getCustomerListByNames(List.of(text));
            if (CollectionUtils.isEmpty(customerList)) {
                return StringUtils.EMPTY;
            }
            return customerList.stream()
                    .filter(customer -> Strings.CS.equals(text, customer.getName()))
                    .map(Customer::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);

        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.OPPORTUNITY.name())) {
            List<Opportunity> opportunityList = Objects.requireNonNull(opportunityService).getOpportunityListByNames(List.of(text));
            if (CollectionUtils.isEmpty(opportunityList)) {
                return StringUtils.EMPTY;
            }
            return opportunityList.stream()
                    .filter(opportunity -> Strings.CS.equals(text, opportunity.getName()))
                    .map(Opportunity::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CLUE.name())) {
            List<Clue> clueList = Objects.requireNonNull(clueService).getClueListByNames(List.of(text));
            if (CollectionUtils.isEmpty(clueList)) {
                return StringUtils.EMPTY;
            }
            return clueList.stream()
                    .filter(clue -> Strings.CS.equals(text, clue.getName()))
                    .map(Clue::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTACT.name())) {
            List<CustomerContact> contactList = Objects.requireNonNull(contactService).getContactListByNames(List.of(text));
            if (CollectionUtils.isEmpty(contactList)) {
                return StringUtils.EMPTY;
            }
            return contactList.stream()
                    .filter(contact -> Strings.CS.equals(text, contact.getName()))
                    .map(CustomerContact::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PRODUCT.name())) {
            List<Product> productList = Objects.requireNonNull(productService).getProductListByNames(List.of(text));
            if (CollectionUtils.isEmpty(productList)) {
                return StringUtils.EMPTY;
            }
            return productList.stream()
                    .filter(product -> Strings.CS.equals(text, product.getName()))
                    .map(Product::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PRICE.name())) {
            List<ProductPrice> productPrices = Objects.requireNonNull(productPriceService).getProductPriceListByNames(List.of(text));
            if (CollectionUtils.isEmpty(productPrices)) {
                return StringUtils.EMPTY;
            }
            return productPrices.stream()
                    .filter(price -> Strings.CS.equals(text, price.getName()))
                    .map(ProductPrice::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.QUOTATION.name())) {
            List<OpportunityQuotation> quotations = Objects.requireNonNull(opportunityQuotationService).getQuotationListByNames(List.of(text));
            if (CollectionUtils.isEmpty(quotations)) {
                return StringUtils.EMPTY;
            }
            return quotations.stream()
                    .filter(quotation -> Strings.CS.equals(text, quotation.getName()))
                    .map(OpportunityQuotation::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTRACT.name())) {
            List<Contract> contracts = Objects.requireNonNull(contractService).getContractListByNames(List.of(text));
            if (CollectionUtils.isEmpty(contracts)) {
                return StringUtils.EMPTY;
            }
            return contracts.stream()
                    .filter(contract -> Strings.CS.equals(text, contract.getName()))
                    .map(Contract::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.PAYMENT_PLAN.name())) {
            List<ContractPaymentPlan> plans = Objects.requireNonNull(contractPaymentPlanService).getPlanListByNames(List.of(text));
            if (CollectionUtils.isEmpty(plans)) {
                return StringUtils.EMPTY;
            }
            return plans.stream()
                    .filter(plan -> Strings.CS.equals(text, plan.getName()))
                    .map(ContractPaymentPlan::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.CONTRACT_PAYMENT_RECORD.name())) {
            List<ContractPaymentRecord> records = Objects.requireNonNull(contractPaymentRecordService).getRecordListByNames(List.of(text));
            if (CollectionUtils.isEmpty(records)) {
                return StringUtils.EMPTY;
            }
            return records.stream()
                    .filter(record -> Strings.CS.equals(text, record.getName()))
                    .map(ContractPaymentRecord::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.BUSINESS_TITLE.name())) {
            List<BusinessTitle> businessTitles = Objects.requireNonNull(businessTitleService).getBusinessTitleListByNames(List.of(text));
            if (CollectionUtils.isEmpty(businessTitles)) {
                return StringUtils.EMPTY;
            }
            return businessTitles.stream()
                    .filter(title -> Strings.CS.equals(text, title.getName()))
                    .map(BusinessTitle::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.ORDER.name())) {
            List<Order> orders = Objects.requireNonNull(orderService).getOrderListByNames(List.of(text));
            if (CollectionUtils.isEmpty(orders)) {
                return StringUtils.EMPTY;
            }
            return orders.stream()
                    .filter(order -> Strings.CS.equals(text, order.getName()))
                    .map(Order::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        if (Strings.CI.equals(field.getDataSourceType(), FieldSourceType.INVOICE.name())) {
            List<ContractInvoice> invoices = Objects.requireNonNull(invoiceService).getContractInvoiceListByNames(List.of(text));
            if (CollectionUtils.isEmpty(invoices)) {
                return StringUtils.EMPTY;
            }
            return invoices.stream()
                    .filter(invoice -> Strings.CS.equals(text, invoice.getName()))
                    .map(ContractInvoice::getId)
                    .findFirst()
                    .orElse(StringUtils.EMPTY);
        }

        List<CustomFormData> customFormDataList = Objects.requireNonNull(customFormDataService).selectByNames(List.of(text));
        if (CollectionUtils.isEmpty(customFormDataList)) {
            return StringUtils.EMPTY;
        }
        return customFormDataList.stream()
                .filter(data -> Strings.CS.equals(text, data.getName()))
                .map(CustomFormData::getId)
                .findFirst()
                .orElse(StringUtils.EMPTY);
    }
}
