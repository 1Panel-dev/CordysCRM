package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModel;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.ResourceLogService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.TextAreaField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ImportRequest;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.idev.excel.FastExcelFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FollowUpImportServiceTest {

    private static final String ORG_ID = "org";
    private static final String USER_ID = "user";
    private static final String RESOURCE_ID = "existing";

    private final FollowUpImportService service = new FollowUpImportService();
    private final LogService logService = mock(LogService.class);
    private final ModuleFormService formService = mock(ModuleFormService.class);
    private final CommonMapper commonMapper = mock(CommonMapper.class);

    @ParameterizedTest
    @EnumSource(ImportType.class)
    void importsRecordWithResourceLog(ImportType type) {
        configureService(FormKey.FOLLOW_RECORD, "recordDescription");
        @SuppressWarnings("unchecked")
        BaseMapper<FollowUpRecord> mapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "followUpRecordMapper", mapper);
        ReflectionTestUtils.setField(service, "followUpRecordFieldService", mock(FollowUpRecordFieldService.class));
        ReflectionTestUtils.setField(service, "followUpRecordFieldMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "followUpRecordFieldBlobMapper", mock(BaseMapper.class));
        FollowUpRecord before = new FollowUpRecord();
        before.setId(RESOURCE_ID);
        before.setOrganizationId(ORG_ID);
        before.setContent("Before");
        when(mapper.insert(any())).thenReturn(1);
        when(mapper.selectByIds(List.of(RESOURCE_ID))).thenReturn(List.of(before));
        when(mapper.update(any())).thenAnswer(call -> {
            FollowUpRecord updated = call.getArgument(0);
            when(mapper.selectByIds(List.of(RESOURCE_ID))).thenReturn(List.of(updated));
            return 1;
        });

        try (MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class);
                MockedStatic<IDGenerator> ids = mockStatic(IDGenerator.class);
                MockedStatic<Translator> translator = mockStatic(Translator.class, call -> call.getArgument(0))) {
            ids.when(IDGenerator::nextStr).thenReturn("new-resource", "field-row");
            beans.when(() -> CommonBeanFactory.getBean(CommonMapper.class)).thenReturn(commonMapper);
            assertEquals(1, service.importRecord(workbook(type), request(type), ORG_ID, USER_ID).getSuccessCount());
        }

        assertLog(type, LogModule.FOLLOW_UP_RECORD);
    }

    @ParameterizedTest
    @EnumSource(ImportType.class)
    void importsPlanWithResourceLog(ImportType type) {
        configureService(FormKey.FOLLOW_PLAN, "planContent");
        @SuppressWarnings("unchecked")
        BaseMapper<FollowUpPlan> mapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "followUpPlanMapper", mapper);
        ReflectionTestUtils.setField(service, "followUpPlanFieldService", mock(FollowUpPlanFieldService.class));
        ReflectionTestUtils.setField(service, "followUpPlanFieldMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "followUpPlanFieldBlobMapper", mock(BaseMapper.class));
        FollowUpPlan before = new FollowUpPlan();
        before.setId(RESOURCE_ID);
        before.setOrganizationId(ORG_ID);
        before.setContent("Before");
        when(mapper.insert(any())).thenReturn(1);
        when(mapper.selectByIds(List.of(RESOURCE_ID))).thenReturn(List.of(before));
        when(mapper.update(any())).thenAnswer(call -> {
            FollowUpPlan updated = call.getArgument(0);
            when(mapper.selectByIds(List.of(RESOURCE_ID))).thenReturn(List.of(updated));
            return 1;
        });

        try (MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class);
                MockedStatic<IDGenerator> ids = mockStatic(IDGenerator.class);
                MockedStatic<Translator> translator = mockStatic(Translator.class, call -> call.getArgument(0))) {
            ids.when(IDGenerator::nextStr).thenReturn("new-resource", "field-row");
            beans.when(() -> CommonBeanFactory.getBean(CommonMapper.class)).thenReturn(commonMapper);
            assertEquals(1, service.importPlan(workbook(type), request(type), ORG_ID, USER_ID).getSuccessCount());
        }

        assertLog(type, LogModule.FOLLOW_UP_PLAN);
    }

    @ParameterizedTest
    @EnumSource(value = FormKey.class, names = {"FOLLOW_RECORD", "FOLLOW_PLAN"})
    void updateImportWritesOnlyProvidedCustomValues(FormKey formKey) {
        boolean record = formKey == FormKey.FOLLOW_RECORD;
        String mapperPrefix = record ? "followUpRecord" : "followUpPlan";
        String fieldTable = record ? "follow_up_record_field" : "follow_up_plan_field";
        configureService(formKey, record ? "recordDescription" : "planContent");
        InputField changed = new InputField();
        InputField blank = new InputField();
        TextAreaField omitted = new TextAreaField();
        configureCustomField(changed, "Changed", "INPUT");
        configureCustomField(blank, "Blank", "INPUT");
        configureCustomField(omitted, "Omitted", "TEXTAREA");
        when(formService.getAllFields(formKey.getKey(), ORG_ID)).thenReturn(List.of(changed, blank, omitted));

        @SuppressWarnings("unchecked")
        BaseMapper<BaseModel> mapper = mock(BaseMapper.class);
        @SuppressWarnings("unchecked")
        BaseMapper<BaseResourceField> fieldMapper = mock(BaseMapper.class);
        BaseMapper<?> blobMapper = mock(BaseMapper.class);
        BaseResourceFieldService<?, ?> fieldService = record
                ? mock(FollowUpRecordFieldService.class) : mock(FollowUpPlanFieldService.class);
        ReflectionTestUtils.setField(service, mapperPrefix + "Mapper", mapper);
        ReflectionTestUtils.setField(service, mapperPrefix + "FieldMapper", fieldMapper);
        ReflectionTestUtils.setField(service, mapperPrefix + "FieldBlobMapper", blobMapper);
        ReflectionTestUtils.setField(service, mapperPrefix + "FieldService", fieldService);
        when(mapper.update(any())).thenReturn(1);
        BaseResourceSubField existing = new BaseResourceSubField();
        existing.setId("existing-field");
        when(commonMapper.getResourceField(fieldTable, RESOURCE_ID, "Changed", null, null)).thenReturn(existing);
        when(fieldMapper.selectByIds(List.of(existing.getId()))).thenReturn(List.of(existing));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FastExcelFactory.write(output).head(List.of(List.of("唯一ID"), List.of("Changed"), List.of("Blank")))
                .sheet().doWrite(List.of(List.of(RESOURCE_ID, "After", "")));
        MockMultipartFile file = new MockMultipartFile("file", "follow-up.xlsx", null, output.toByteArray());
        ImportRequest request = request(ImportType.UPDATE);
        try (MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class);
                MockedStatic<IDGenerator> ids = mockStatic(IDGenerator.class);
                MockedStatic<Translator> translator = mockStatic(Translator.class, call -> call.getArgument(0))) {
            ids.when(IDGenerator::nextStr).thenReturn("generated");
            beans.when(() -> CommonBeanFactory.getBean(CommonMapper.class)).thenReturn(commonMapper);
            assertEquals(1, (record ? service.importRecord(file, request, ORG_ID, USER_ID)
                    : service.importPlan(file, request, ORG_ID, USER_ID)).getSuccessCount());
        }

        ArgumentCaptor<BaseResourceField> updated = ArgumentCaptor.forClass(BaseResourceField.class);
        verify(fieldMapper).selectByIds(List.of(existing.getId()));
        verify(fieldMapper).update(updated.capture());
        assertEquals(existing.getId(), updated.getValue().getId());
        assertEquals("Changed", updated.getValue().getFieldId());
        assertEquals("After", updated.getValue().getFieldValue());
        verifyNoMoreInteractions(fieldMapper);
        verifyNoInteractions(blobMapper);
        verify(fieldService, never()).deleteByResourceIds(any());
    }

    private void configureCustomField(BaseField field, String name, String type) {
        field.setId(name);
        field.setName(name);
        field.setType(type);
        field.setReadable(true);
        field.setEditable(true);
    }

    private void configureService(FormKey formKey, String internalKey) {
        InputField content = new InputField();
        content.setId("content");
        content.setName("Content");
        content.setType("INPUT");
        content.setInternalKey(internalKey);
        content.setReadable(true);
        content.setEditable(true);
        when(formService.getAllFields(formKey.getKey(), ORG_ID)).thenReturn(List.of(content));
        when(commonMapper.checkIdCount(any(), any())).thenReturn(1);
        BaseService baseService = new BaseService();
        ReflectionTestUtils.setField(baseService, "resourceLogService", new ResourceLogService());
        ReflectionTestUtils.setField(service, "baseService", baseService);
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "moduleFormService", formService);
    }

    private static MockMultipartFile workbook(ImportType type) {
        boolean update = type == ImportType.UPDATE;
        List<List<String>> headers = update ? List.of(List.of("唯一ID"), List.of("Content"))
                : List.of(List.of("Content"));
        List<String> row = update ? List.of(RESOURCE_ID, "After") : List.of("After");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FastExcelFactory.write(output).head(headers).sheet().doWrite(List.of(row));
        return new MockMultipartFile("file", "follow-up.xlsx", null, output.toByteArray());
    }

    private static ImportRequest request(ImportType type) {
        ImportRequest request = new ImportRequest();
        request.setImportType(type.name());
        return request;
    }

    private void assertLog(ImportType type, String module) {
        ArgumentCaptor<List<LogDTO>> captor = ArgumentCaptor.captor();
        verify(logService).batchAdd(captor.capture());
        assertEquals(1, captor.getValue().size());
        LogDTO log = captor.getValue().getFirst();
        assertEquals(module, log.getModule());
        assertEquals(ORG_ID, log.getOrganizationId());
        assertEquals(USER_ID, log.getCreateUser());
        assertEquals(type == ImportType.ADD ? LogType.ADD : LogType.UPDATE, log.getType());
        if (type == ImportType.UPDATE) {
            assertEquals(RESOURCE_ID, log.getResourceId());
            assertEquals("Before", ((Map<?, ?>) log.getOriginalValue()).get("content"));
            assertEquals("After", ((Map<?, ?>) log.getModifiedValue()).get("content"));
        }
    }
}
