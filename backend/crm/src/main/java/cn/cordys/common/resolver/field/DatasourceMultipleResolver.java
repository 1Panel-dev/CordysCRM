package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.system.domain.Product;
import cn.cordys.crm.system.dto.field.DatasourceMultipleField;
import cn.cordys.crm.system.service.ProductService;
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
        List list = JSON.parseArray(value, String.class);

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), "CUSTOMER")) {
            return Objects.requireNonNull(customerService).getCustomerNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), "CONTACT")) {
            return Objects.requireNonNull(contactService).getContactNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), "OPPORTUNITY")) {
            return Objects.requireNonNull(opportunityService).getOpportunityNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), "CLUE")) {
            return Objects.requireNonNull(clueService).getClueNameByIds(list);
        }

        if (Strings.CI.equals(datasourceMultipleField.getDataSourceType(), "PRODUCT")) {
            return Objects.requireNonNull(productService).getProductNameByIds(list);
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
            List<Customer> customerList = Objects.requireNonNull(customerService).getCustomerListByNames(names);
            List<String> ids = customerList.stream().map(Customer::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), "OPPORTUNITY")) {
            List<Opportunity> opportunityList = Objects.requireNonNull(opportunityService).getOpportunityListByNames(names);
            List<String> ids = opportunityList.stream().map(Opportunity::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), "CLUE")) {
            List<Clue> clueList = Objects.requireNonNull(clueService).getClueListByNames(names);
            List<String> ids = clueList.stream().map(Clue::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), "CONTACT")) {
            List<CustomerContact> contactList = Objects.requireNonNull(contactService).getContactListByNames(names);
            List<String> ids = contactList.stream().map(CustomerContact::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        if (Strings.CI.equals(field.getDataSourceType(), "PRODUCT")) {
            List<Product> productList = Objects.requireNonNull(productService).getProductListByNames(names);
            List<String> ids = productList.stream().map(Product::getId).toList();
            return CollectionUtils.isEmpty(ids) ? names : ids;
        }
        return names;
    }
}
