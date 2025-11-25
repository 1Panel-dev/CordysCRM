package cn.cordys.crm.contract;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanGetResponse;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import cn.cordys.common.util.BeanUtils;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContractPaymentPlanControllerTests extends BaseTest {
    private static final String BASE_PATH = "/contract/payment-plan/";

    private static ContractPaymentPlan addContractPaymentPlan;

    @Resource
    private BaseMapper<ContractPaymentPlan> contractPaymentPlanMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testPageEmpty() throws Exception {
        ContractPaymentPlanPageRequest request = new ContractPaymentPlanPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<ContractPaymentPlanListResponse>> pageResult = getPageResult(mvcResult, ContractPaymentPlanListResponse.class);
        List<ContractPaymentPlanListResponse> contractPaymentPlanList = pageResult.getList();
        Assertions.assertTrue(CollectionUtils.isEmpty(contractPaymentPlanList));

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        // 请求成功
        ContractPaymentPlanAddRequest request = new ContractPaymentPlanAddRequest();
        request.setPlanAmount(BigDecimal.valueOf(111));
        request.setOwner(InternalUser.ADMIN.getValue());
        request.setPlanStatus("test");
        request.setContractId("test");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        ContractPaymentPlan resultData = getResultData(mvcResult, ContractPaymentPlan.class);
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        this.addContractPaymentPlan = contractPaymentPlan;
        Assertions.assertEquals(request.getPlanAmount().intValue(), contractPaymentPlan.getPlanAmount().intValue());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CONTRACT_PAYMENT_PLAN_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        ContractPaymentPlanUpdateRequest request = new ContractPaymentPlanUpdateRequest();
        request.setId(addContractPaymentPlan.getId());
        request.setPlanAmount(BigDecimal.valueOf(222));
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        ContractPaymentPlan userContractPaymentPlanResult = contractPaymentPlanMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getPlanAmount().intValue(), userContractPaymentPlanResult.getPlanAmount().intValue());

        // 不修改信息
        ContractPaymentPlanUpdateRequest emptyRequest = new ContractPaymentPlanUpdateRequest();
        emptyRequest.setId(addContractPaymentPlan.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CONTRACT_PAYMENT_PLAN_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(3)
    void testGet() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addContractPaymentPlan.getId());
        ContractPaymentPlanGetResponse getResponse = getResultData(mvcResult, ContractPaymentPlanGetResponse.class);

        // 校验请求成功数据
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(addContractPaymentPlan.getId());
        ContractPaymentPlan responseContractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), getResponse);
        Assertions.assertEquals(responseContractPaymentPlan, contractPaymentPlan);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ, DEFAULT_GET, addContractPaymentPlan.getId());
    }

    @Test
    @Order(4)
    void testPage() throws Exception {
        ContractPaymentPlanPageRequest request = new ContractPaymentPlanPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<ContractPaymentPlanListResponse>> pageResult = getPageResult(mvcResult, ContractPaymentPlanListResponse.class);
        List<ContractPaymentPlanListResponse> contractPaymentPlanList = pageResult.getList();
        
        // 校验权限
        requestPostPermissionTest(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(10)
    void delete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addContractPaymentPlan.getId());
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(addContractPaymentPlan.getId());
        Assertions.assertNull(contractPaymentPlan);
        // 校验权限
        requestGetPermissionTest(PermissionConstants.CONTRACT_PAYMENT_PLAN_DELETE, DEFAULT_DELETE, "1111");
    }
}