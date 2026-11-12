package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.dto.request.CustomerAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerExportRequest;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.dto.request.ExportTaskCenterQueryRequest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ExportTaskCenterControllerTest extends BaseTest {

    private static final String LIST = "/export/center/list";
    private static final String CANCEL = "/export/center/cancel/";
    private static final String DOWN_LOAD = "/export/center/download/";

    @Resource
    private BaseMapper<ExportTask> exportTaskMapper;

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;

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

    @Test
    void testCancel() throws Exception {
        this.requestGet(CANCEL + "missing-task").andExpect(status().is5xxServerError());
        // 预置待执行任务，避免小文件导出先完成导致取消操作偶发失败。
        ExportTask task = new ExportTask();
        task.setId(IDGenerator.nextStr());
        task.setFileName("cancel-export-test");
        task.setFileId(IDGenerator.nextStr());
        task.setResourceType(ExportConstants.ExportType.CUSTOMER.name());
        task.setStatus(ExportConstants.ExportStatus.PREPARED.name());
        task.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        task.setCreateUser("admin");
        task.setUpdateUser("admin");
        task.setCreateTime(System.currentTimeMillis());
        task.setUpdateTime(task.getCreateTime());
        exportTaskMapper.insert(task);
        try {
            this.requestGetWithOk(CANCEL + task.getId());
            assertEquals(ExportConstants.ExportStatus.STOP.name(),
                    exportTaskMapper.selectByPrimaryKey(task.getId()).getStatus());
        } finally {
            exportTaskMapper.deleteByPrimaryKey(task.getId());
        }
    }

    @Test
    void downloadsCompletedExport() throws Exception {
        ModuleForm moduleForm = getModuleForm();

        ModuleField example = new ModuleField();
        example.setFormId(moduleForm.getId());
        ModuleField moduleField = moduleFieldMapper.select(example)
                .stream()
                .filter(field -> Strings.CS.equals(field.getInternalKey(), "customerLevel"))
                .findFirst().orElseThrow();

        // 请求成功
        CustomerAddRequest request = new CustomerAddRequest();
        request.setName("testTask");
        request.setOwner("admin");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleField.getId(), "1")));

        Customer customer = getResultData(this.requestPostWithOkAndReturn("/account/add", request), Customer.class);

        CustomerExportRequest requestE = new CustomerExportRequest();
        requestE.setCurrent(1);
        requestE.setPageSize(10);
        requestE.setFileName("测试导出客户");

        ExportHeadDTO exportHeadDTO = new ExportHeadDTO();
        exportHeadDTO.setKey("name");
        exportHeadDTO.setTitle("客户名称");
        requestE.setHeadList(List.of(exportHeadDTO));
        try {
            MvcResult result = this.requestPostWithOkAndReturn("/account/export-all", requestE);
            String taskId = getResultData(result, String.class);
            await().alias("客户导出完成").atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertEquals(ExportConstants.ExportStatus.SUCCESS.name(),
                            exportTaskMapper.selectByPrimaryKey(taskId).getStatus()));
            this.requestGetStreamWithOk(DOWN_LOAD + taskId);
        } finally {
            this.requestGetWithOk("/account/delete/" + customer.getId());
        }
    }

}
