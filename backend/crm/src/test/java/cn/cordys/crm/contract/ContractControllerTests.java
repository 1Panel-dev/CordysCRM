package cn.cordys.crm.contract;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.dto.request.ContractAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.contract.service.ContractFieldService;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ContractControllerTests extends BaseTest {

    protected static final String MODULE_FORM = "module/form";
    protected static final String BATCH_UPDATE = "batch/update";
    private static final String BASE_PATH = "/contract/";

    private static Contract addContract;
    private static ModuleFormConfigDTO moduleFormConfig;

    @Resource
    private BaseMapper<Contract> contractMapper;

    @Resource
    private ContractFieldService contractFieldService;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testModuleField() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(MODULE_FORM);
        moduleFormConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CONTRACT_READ, MODULE_FORM);
    }

    @Sql(scripts = {"/dml/init_contract_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @Order(1)
    void testAdd() throws Exception {
        ContractAddRequest request = new ContractAddRequest();
        request.setName("测试合同1");
        request.setCustomerId("contract_test_customer");
        request.setOwner(InternalUser.ADMIN.getValue());
        request.setStartTime(System.currentTimeMillis());
        request.setEndTime(System.currentTimeMillis() + 86400000L);
        // ContractService.add() 要求必须传 moduleFields 和 moduleFormConfigDTO
        // 从 moduleFormConfig 中构造字段值，将每个字段的 id 与对应的业务值配对
        List<BaseModuleFieldValue> moduleFields = new ArrayList<>();
        for (BaseField field : moduleFormConfig.getFields()) {
            Object value = switch (field.getInternalKey()) {
                case "contractName" -> request.getName();
                case "contractOwner" -> request.getOwner();
                case "contractCustomer" -> request.getCustomerId();
                case "contractStartTime" -> request.getStartTime();
                case "contractEndTime" -> request.getEndTime();
                default -> null;
            };
            if (value != null) {
                moduleFields.add(new BaseModuleFieldValue(field.getId(), value));
            }
        }
        request.setModuleFields(moduleFields);
        request.setModuleFormConfigDTO(moduleFormConfig);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Contract resultData = getResultData(mvcResult, Contract.class);
        addContract = contractMapper.selectByPrimaryKey(resultData.getId());

        // 校验合同数据写入
        Assertions.assertNotNull(addContract);
        Assertions.assertEquals(request.getName(), addContract.getName());
        Assertions.assertEquals(request.getOwner(), addContract.getOwner());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CONTRACT_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testGet() throws Exception {
        this.requestGetWithOk(DEFAULT_GET, addContract.getId());

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CONTRACT_READ, DEFAULT_GET, addContract.getId());
    }

    @Test
    @Order(3)
    void testPage() throws Exception {
        ContractPageRequest request = new ContractPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        this.requestPostWithOk(DEFAULT_PAGE, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CONTRACT_READ, DEFAULT_PAGE, request);
    }
    @Test
    @Order(4)
    void testBatchUpdate() throws Exception {
        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        for (BaseField field : moduleFormConfig.getFields()) {
            request.setIds(List.of(addContract.getId()));
            request.setFieldId(field.getId());

            String internalKey = field.getInternalKey();

            if (Strings.CS.equals(internalKey, BusinessModuleField.CONTRACT_NAME.getKey())) {
                // 更新合同名称
                String newName = "批量更新合同名称_" + UUID.randomUUID().toString().substring(0, 8);
                request.setFieldValue(newName);
                this.requestPostWithOk(BATCH_UPDATE, request);

                Contract updated = contractMapper.selectByPrimaryKey(addContract.getId());
                Assertions.assertNotNull(updated);
                Assertions.assertEquals(newName, updated.getName(), "批量更新合同名称后，DB 中的 name 应等于请求值");

            } else if (Strings.CS.equals(internalKey, BusinessModuleField.CONTRACT_OWNER.getKey())) {
                // 更新负责人：改成 permission_test，断言值确实发生变化
                String newOwner = PERMISSION_USER_NAME;
                request.setFieldValue(newOwner);
                this.requestPostWithOk(BATCH_UPDATE, request);

                Contract updated = contractMapper.selectByPrimaryKey(addContract.getId());
                Assertions.assertNotNull(updated);
                Assertions.assertNotEquals(InternalUser.ADMIN.getValue(), updated.getOwner(), "负责人应已从 admin 更新为其他用户");
                Assertions.assertEquals(newOwner, updated.getOwner(), "批量更新负责人后，DB 中的 owner 应等于请求值");

            } else if (Strings.CS.equals(internalKey, BusinessModuleField.CONTRACT_CUSTOMER_NAME.getKey())) {
                // 更新关联客户
                request.setFieldValue("contract_test_customer");
                this.requestPostWithOk(BATCH_UPDATE, request);

                Contract updated = contractMapper.selectByPrimaryKey(addContract.getId());
                Assertions.assertNotNull(updated);
                Assertions.assertEquals("contract_test_customer", updated.getCustomerId(), "批量更新客户后，DB 中的 customer_id 应等于请求值");

            } else if (Strings.CS.equals(internalKey, BusinessModuleField.CONTRACT_TOTAL_AMOUNT.getKey())) {
                // 更新金额
                request.setFieldValue("9999.99");
                this.requestPostWithOk(BATCH_UPDATE, request);

                Contract updated = contractMapper.selectByPrimaryKey(addContract.getId());
                Assertions.assertNotNull(updated);
                Assertions.assertEquals(0, new java.math.BigDecimal("9999.99").compareTo(updated.getAmount()), "批量更新金额后，DB 中的 amount 应等于请求值");

            } else if (Strings.CS.equals(internalKey, BusinessModuleField.CONTRACT_NO.getKey())) {
                // 更新编号
                String newNumber = "HT-" + UUID.randomUUID().toString().substring(0, 8);
                request.setFieldValue(newNumber);
                this.requestPostWithOk(BATCH_UPDATE, request);

                Contract updated = contractMapper.selectByPrimaryKey(addContract.getId());
                Assertions.assertNotNull(updated);
                Assertions.assertEquals(newNumber, updated.getNumber(), "批量更新编号后，DB 中的 number 应等于请求值");

            } else if (Strings.CS.equals(internalKey, BusinessModuleField.CONTRACT_START_TIME.getKey())) {
                // 更新开始时间
                Long newStartTime = System.currentTimeMillis() + 100000L;
                request.setFieldValue(newStartTime);
                this.requestPostWithOk(BATCH_UPDATE, request);

                Contract updated = contractMapper.selectByPrimaryKey(addContract.getId());
                Assertions.assertNotNull(updated);
                Assertions.assertEquals(newStartTime, updated.getStartTime(), "批量更新开始时间后，DB 中的 start_time 应等于请求值");

            } else if (Strings.CS.equals(internalKey, BusinessModuleField.CONTRACT_END_TIME.getKey())) {
                // 更新结束时间
                Long newEndTime = System.currentTimeMillis() + 200000L;
                request.setFieldValue(newEndTime);
                this.requestPostWithOk(BATCH_UPDATE, request);

                Contract updated = contractMapper.selectByPrimaryKey(addContract.getId());
                Assertions.assertNotNull(updated);
                Assertions.assertEquals(newEndTime, updated.getEndTime(), "批量更新结束时间后，DB 中的 end_time 应等于请求值");

            }
        }

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CONTRACT_UPDATE, BATCH_UPDATE, request);
    }

    @Test
    @Order(10)
    void testDelete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addContract.getId());
        Contract contract = contractMapper.selectByPrimaryKey(addContract.getId());
        Assertions.assertNull(contract);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CONTRACT_DELETE, DEFAULT_DELETE, "nonexistent-id");
    }
}
