package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerField;
import cn.cordys.crm.customer.dto.request.CustomerAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerExportRequest;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.dto.request.ExportTaskCenterQueryRequest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExportTaskCenterControllerTest extends BaseTest {

    private static final String LIST = "/export/center/list";
    private static final String CANCEL = "/export/center/cancel/";
    private static final String DOWN_LOAD = "/export/center/download/";

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Resource
    private BaseMapper<CustomerField> customerFieldMapper;

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;



    @Order(1)
    @Test
    void testList() throws Exception {
        ExportTaskCenterQueryRequest exportTaskCenterQueryRequest = new ExportTaskCenterQueryRequest();
        this.requestPostWithOk(LIST, exportTaskCenterQueryRequest);
    }

    private ModuleForm getModuleForm() {
        ModuleForm example = new ModuleForm();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        example.setFormKey(FormKey.CUSTOMER.getKey());
        return moduleFormMapper.selectOne(example);
    }

    @Order(1)
    @Test
    void testCancel() throws Exception {
        this.requestGet(CANCEL+"/rr").andExpect(status().is5xxServerError());
        ModuleForm moduleForm = getModuleForm();

        ModuleField example = new ModuleField();
        example.setFormId(moduleForm.getId());
        ModuleField moduleField = moduleFieldMapper.select(example)
                .stream()
                .filter(field -> Strings.CS.equals(field.getInternalKey(), "customerLevel"))
                .findFirst().orElse(null);

        // 请求成功
        CustomerAddRequest request = new CustomerAddRequest();
        request.setName("testTask");
        request.setOwner("bb");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleField.getId(), "1")));

        this.requestPostWithOkAndReturn("/account/add", request);

        CustomerExportRequest requestE = new CustomerExportRequest();
        requestE.setCurrent(1);
        requestE.setPageSize(10);
        requestE.setFileName("测试导出客户");

        ExportHeadDTO exportHeadDTO = new ExportHeadDTO();
        exportHeadDTO.setKey("name");
        exportHeadDTO.setTitle("客户名称");
        List<ExportHeadDTO> list = new ArrayList<>();
        list.add(exportHeadDTO);
        requestE.setHeadList(list);
        MvcResult mvcResult2 = this.requestPostWithOkAndReturn("/account/export-all", requestE);
        String resultData = getResultData(mvcResult2, String.class);
        this.requestGetWithOk(CANCEL+resultData);
        Thread.sleep(1500);
        mvcResult2 = this.requestPostWithOkAndReturn("/account/export-all", requestE);
        resultData = getResultData(mvcResult2, String.class);
        Thread.sleep(1500);
        // 等待导出任务完成
        this.requestGetStreamWithOk(DOWN_LOAD+resultData);


    }


}
