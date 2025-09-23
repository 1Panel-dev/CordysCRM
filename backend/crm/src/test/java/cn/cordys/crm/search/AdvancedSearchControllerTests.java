package cn.cordys.crm.search;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.domain.CustomerField;
import cn.cordys.crm.customer.dto.request.CustomerAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerContactAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.search.response.advanced.AdvancedCustomerContactResponse;
import cn.cordys.crm.search.response.advanced.AdvancedCustomerResponse;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdvancedSearchControllerTests extends BaseTest {


    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<CustomerField> customerFieldMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<CustomerContact> customerContactMapper;

    void addCustomer() throws Exception {
        ModuleForm moduleForm = getModuleForm();

        ModuleField example = new ModuleField();
        example.setFormId(moduleForm.getId());
        ModuleField moduleField = moduleFieldMapper.select(example)
                .stream()
                .filter(field -> Strings.CS.equals(field.getInternalKey(), "customerLevel"))
                .findFirst().orElse(null);

        // 请求成功
        CustomerAddRequest request = new CustomerAddRequest();
        request.setName("aa1");
        request.setOwner("bb1");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleField.getId(), "1")));

        MvcResult mvcResult = this.requestPostWithOkAndReturn("/account/add", request);
        Customer resultData = getResultData(mvcResult, Customer.class);
        Customer customer = customerMapper.selectByPrimaryKey(resultData.getId());
        Assertions.assertEquals(request.getName(), customer.getName());
        Assertions.assertEquals(request.getOwner(), customer.getOwner());

        // 校验字段
        List<BaseModuleFieldValue> fieldValues = getCustomerFields(customer.getId())
                .stream().map(customerField -> {
                    BaseModuleFieldValue baseModuleFieldValue = BeanUtils.copyBean(new BaseModuleFieldValue(), customerField);
                    baseModuleFieldValue.setFieldValue(customerField.getFieldValue().toString());
                    return baseModuleFieldValue;
                })
                .toList();
        Assertions.assertEquals(request.getModuleFields(), fieldValues);

        // 创建另一个客户
        request.setName("another1");
        request.setOwner(InternalUser.ADMIN.getValue());
        this.requestPostWithOkAndReturn("/account/add", request);

    }

    private List<CustomerField> getCustomerFields(String customerId) {
        CustomerField example = new CustomerField();
        example.setResourceId(customerId);
        return customerFieldMapper.select(example);
    }

    private ModuleForm getModuleForm() {
        ModuleForm example = new ModuleForm();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        example.setFormKey(FormKey.CUSTOMER.getKey());
        return moduleFormMapper.selectOne(example);
    }

    void addCustomerContact() throws Exception {
        // 请求成功
        CustomerContactAddRequest request = new CustomerContactAddRequest();
        request.setName("test");
        request.setCustomerId("customerId");
        request.setOwner(InternalUser.ADMIN.getValue());
        request.setPhone("15451222354");
        this.requestPostWithOkAndReturn("/account/contact/add", request);
    }


    @Test
    @Order(0)
    void testCustomer() throws Exception {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        this.requestPostWithOkAndReturn("/advanced/search/account", request);

        addCustomer();

        MvcResult mvcResult = this.requestPostWithOkAndReturn("/advanced/search/account", request);
        Pager<List<AdvancedCustomerResponse>> pageResult = getPageResult(mvcResult, AdvancedCustomerResponse.class);
        List<AdvancedCustomerResponse> customerList = pageResult.getList();
        Assertions.assertFalse(customerList.isEmpty());
    }

    @Test
    @Order(1)
    void testCustomerContact() throws Exception {
        CustomerContactPageRequest request = new CustomerContactPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        this.requestPostWithOkAndReturn("/advanced/search/contact", request);
        addCustomerContact();
        MvcResult mvcResult = this.requestPostWithOkAndReturn("/advanced/search/contact", request);
        Pager<List<AdvancedCustomerContactResponse>> pageResult = getPageResult(mvcResult, AdvancedCustomerContactResponse.class);
        List<AdvancedCustomerContactResponse> customerList = pageResult.getList();
        Assertions.assertFalse(customerList.isEmpty());

    }


}
