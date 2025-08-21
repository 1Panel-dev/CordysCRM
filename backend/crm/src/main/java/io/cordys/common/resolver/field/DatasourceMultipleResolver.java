package io.cordys.common.resolver.field;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.service.ClueService;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.service.CustomerContactService;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.service.OpportunityService;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.dto.field.DatasourceMultipleField;
import io.cordys.crm.system.service.ProductService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;

public class DatasourceMultipleResolver extends AbstractModuleFieldResolver<DatasourceMultipleField> {

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
        if (StringUtils.isBlank(value) || Strings.CS.equals(value, "[]")) {
            return StringUtils.EMPTY;
        }
        List list = JSON.parseArray(value,String.class);

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), "CUSTOMER")) {
            return customerService.getCustomerNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), "OPPORTUNITY")) {
            return opportunityService.getOpportunityNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), "CLUE")) {
            return clueService.getClueNameByIds(list);
        }

        return StringUtils.EMPTY;
    }

    @Override
    public Object text2Value(DatasourceMultipleField field, String text) {
        if (StringUtils.isBlank(text) || Strings.CS.equals(text, "[]")) {
            return StringUtils.EMPTY;
        }
        List<String> names = parseFakeJsonArray(text);

        if (Strings.CI.equals(field.getDataSourceType(), "CUSTOMER")) {
            List<Customer> customerList = customerService.getCustomerListByNames(names);
            List<String> ids = customerList.stream().map(Customer::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if(Strings.CI.equals(field.getDataSourceType(), "OPPORTUNITY")) {
            List<Opportunity> opportunityList = opportunityService.getOpportunityListByNames(names);
            List<String> ids = opportunityList.stream().map(Opportunity::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), "CLUE")) {
            List<Clue> clueList = clueService.getClueListByNames(names);
            List<String> ids = clueList.stream().map(Clue::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), "CONTACT")) {
            List<CustomerContact> contactList = contactService.getContactListByNames(names);
            List<String> ids = contactList.stream().map(CustomerContact::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), "PRODUCT")) {
            List<Product> productList = productService.getProductListByNames(names);
            List<String> ids = productList.stream().map(Product::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        return names;
    }
}
