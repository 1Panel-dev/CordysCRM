package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.product.domain.Product;
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

    static {
        customerService = CommonBeanFactory.getBean(CustomerService.class);
        opportunityService = CommonBeanFactory.getBean(OpportunityService.class);
        clueService = CommonBeanFactory.getBean(ClueService.class);
        contactService = CommonBeanFactory.getBean(CustomerContactService.class);
        productService = CommonBeanFactory.getBean(ProductService.class);
        productPriceService = CommonBeanFactory.getBean(ProductPriceService.class);
        opportunityQuotationService = CommonBeanFactory.getBean(OpportunityQuotationService.class);
    }

    @Override
    public void validate(DatasourceField customField, Object value) {

    }


    @Override
    public Object transformToValue(DatasourceField datasourceField, String value) {
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), "CUSTOMER")) {
            return Objects.requireNonNull(customerService).getCustomerName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), "CONTACT")) {
            return Objects.requireNonNull(contactService).getContactName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), "OPPORTUNITY")) {
            return Objects.requireNonNull(opportunityService).getOpportunityName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), "CLUE")) {
            return Objects.requireNonNull(clueService).getClueName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), "PRODUCT")) {
            return Objects.requireNonNull(productService).getProductName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.PRICE.name())) {
            return Objects.requireNonNull(productPriceService).getProductPriceName(value);
        }

        if (Strings.CI.equals(datasourceField.getDataSourceType(), FieldSourceType.QUOTATION.name())) {
            return Objects.requireNonNull(opportunityQuotationService).getQuotationName(value);
        }

        return StringUtils.EMPTY;
    }

    @Override
    public Object textToValue(DatasourceField field, String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        if (Strings.CI.equals(field.getDataSourceType(), "CUSTOMER")) {
            List<Customer> customerList = Objects.requireNonNull(customerService).getCustomerListByNames(List.of(text));
            return CollectionUtils.isEmpty(customerList) ? text : customerList.getFirst().getId();
        }
        if (Strings.CI.equals(field.getDataSourceType(), "OPPORTUNITY")) {
            List<Opportunity> opportunityList = Objects.requireNonNull(opportunityService).getOpportunityListByNames(List.of(text));
            return CollectionUtils.isEmpty(opportunityList) ? text : opportunityList.getFirst().getId();
        }
        if (Strings.CI.equals(field.getDataSourceType(), "CLUE")) {
            List<Clue> clueList = Objects.requireNonNull(clueService).getClueListByNames(List.of(text));
            return CollectionUtils.isEmpty(clueList) ? text : clueList.getFirst().getId();
        }
        if (Strings.CI.equals(field.getDataSourceType(), "CONTACT")) {
            List<CustomerContact> contactList = Objects.requireNonNull(contactService).getContactListByNames(List.of(text));
            return CollectionUtils.isEmpty(contactList) ? text : contactList.getFirst().getId();
        }
        if (Strings.CI.equals(field.getDataSourceType(), "PRODUCT")) {
            List<Product> productList = Objects.requireNonNull(productService).getProductListByNames(List.of(text));
            return CollectionUtils.isEmpty(productList) ? text : productList.getFirst().getId();
        }
        return text;
    }
}
