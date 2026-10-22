package cn.cordys.common.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.service.ContractPaymentPlanFieldService;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.domain.CustomFormDataField;
import cn.cordys.crm.form.domain.CustomFormDataFieldBlob;
import cn.cordys.crm.form.service.CustomFormDataFieldService;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.InputNumberField;
import cn.cordys.crm.system.dto.field.ProductSubField;
import cn.cordys.crm.system.dto.field.TextAreaField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.RuleProp;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseResourceFieldServiceTest {
    private ApplicationContext previousContext;
    private MessageSource previousMessageSource;

    @BeforeEach
    void saveContext() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        new Translator().setMessageSource(new StaticMessageSource());
    }

    @AfterEach
    void cleanContext() {
        CustomFormDataFieldService.clearFormKey();
        OrganizationContext.clear();
        new CommonBeanFactory().setApplicationContext(previousContext);
        new Translator().setMessageSource(previousMessageSource);
    }

    @ParameterizedTest
    @ValueSource(strings = { "SUB_PRODUCT", "SUB_PRICE", "SERIAL_NUMBER", "FORMULA", "STATISTIC", "ATTACHMENT",
            "PICTURE", "DIVIDER" })
    void batchUpdateRejectsUnsupportedFieldTypes(String type) {
        InputField field = new InputField();
        field.setType(type);
        assertThrows(GenericException.class, () -> checkBatchField(field, "新值"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "system", "readonly", "child", "reference" })
    void batchUpdateRejectsDisplayAndSubtableChildFields(String scope) {
        InputField field = new InputField();
        field.setType("INPUT");
        switch (scope) {
        case "system" -> field.setSys(true);
        case "readonly" -> field.setEditable(false);
        case "child" -> field.setSubTableFieldId("subtable");
        case "reference" -> field.setResourceFieldId("source");
        }
        assertThrows(GenericException.class, () -> checkBatchField(field, "新值"));
    }

    @Test
    void batchUpdateRejectsSubtableChildWithoutParentMarker() {
        InputField child = new InputField();
        child.setType("INPUT");
        ProductSubField subtable = new ProductSubField();
        subtable.setId("subtable");
        subtable.setType("SUB_PRODUCT");
        subtable.setSubFields(List.of(child));
        assertThrows(GenericException.class, () -> checkBatchField(child, "新值", List.of(subtable)));
        assertEquals(child, new ContractPaymentPlanFieldService().getAndCheckField("field-1", "org-1"));
    }

    @Test
    void batchUpdateValidatesRequiredAndNumberValuesAndResolvesBusinessKey() {
        InputField name = new InputField();
        name.setType("INPUT");
        name.setInternalKey("contractPaymentPlanName");
        name.setRules(List.of(new RuleProp("required")));
        assertThrows(GenericException.class, () -> checkBatchField(name, " "));
        assertEquals("name", checkBatchField(name, "新名称").getBusinessKey());

        InputNumberField amount = new InputNumberField();
        amount.setType("INPUT_NUMBER");
        assertThrows(GenericException.class, () -> checkBatchField(amount, "无效金额"));
        assertEquals(amount, checkBatchField(amount, 100));

        TextAreaField note = new TextAreaField();
        note.setType("TEXTAREA");
        assertEquals(note, checkBatchField(note, null));
    }

    private BaseField checkBatchField(BaseField field, Object value) {
        return checkBatchField(field, value, List.of(field));
    }

    private BaseField checkBatchField(BaseField field, Object value, List<BaseField> topLevelFields) {
        ModuleFormService forms = mock(ModuleFormService.class);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(ModuleFormService.class)).thenReturn(forms);
        new CommonBeanFactory().setApplicationContext(context);
        field.setId("field-1");
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(topLevelFields);
        when(forms.getBusinessFormConfig(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), "org-1"))
                .thenReturn(config);
        when(forms.getFlattenFormFields(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), "org-1"))
                .thenReturn(List.of(field));
        doCallRealMethod().when(forms).setFieldBusinessParam(any());
        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setFieldId(field.getId());
        request.setFieldValue(value);
        return new ContractPaymentPlanFieldService().getAndCheckBatchUpdateField(request, "org-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateExistingBlobFieldUpdatesInsteadOfInsertingDuplicate() {
        BaseMapper<CustomFormDataField> fieldMapper = mock(BaseMapper.class);
        BaseMapper<CustomFormDataFieldBlob> blobMapper = mock(BaseMapper.class);
        ModuleFormService moduleFormService = mock(ModuleFormService.class);
        ApplicationContext context = mock(ApplicationContext.class);
        CustomFormDataFieldService service = new CustomFormDataFieldService();
        ReflectionTestUtils.setField(service, "fieldReferenceService", new FieldReferenceService());
        ReflectionTestUtils.setField(
                service, "customFormDataFieldMapper", fieldMapper);
        ReflectionTestUtils.setField(
                service, "customFormDataFieldBlobMapper", blobMapper);

        TextAreaField field = new TextAreaField();
        field.setId("description-field");
        field.setName("说明");
        field.setType("TEXTAREA");
        when(moduleFormService.getAllFields("form-1", "org-1"))
                .thenReturn(List.of(field));
        when(context.getBean(ModuleFormService.class))
                .thenReturn(moduleFormService);
        new CommonBeanFactory().setApplicationContext(context);
        OrganizationContext.setOrganizationId("org-1");
        CustomFormDataFieldService.setFormKey("form-1");

        CustomFormDataFieldBlob persisted = new CustomFormDataFieldBlob();
        persisted.setId("blob-row-1");
        persisted.setResourceId("record-1");
        persisted.setFieldId("description-field");
        persisted.setFieldValue("旧说明");
        when(fieldMapper.selectListByLambda(any())).thenReturn(List.of());
        when(blobMapper.selectListByLambda(any()))
                .thenReturn(List.of(persisted));

        CustomFormData record = new CustomFormData();
        record.setId("record-1");
        service.updateModuleField(
                record,
                "org-1",
                "user-1",
                List.of(new BaseModuleFieldValue(
                        "description-field", "新说明")),
                true);

        ArgumentCaptor<CustomFormDataFieldBlob> updated = ArgumentCaptor.forClass(CustomFormDataFieldBlob.class);
        verify(blobMapper).update(updated.capture());
        assertEquals("blob-row-1", updated.getValue().getId());
        assertEquals("新说明", updated.getValue().getFieldValue());
        verify(blobMapper, never()).batchInsert(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void singleResourceReadRemainsTolerant() {
        BaseMapper<CustomFormDataField> fieldMapper = mock(BaseMapper.class);
        BaseMapper<CustomFormDataFieldBlob> blobMapper = mock(BaseMapper.class);
        ModuleFormService moduleFormService = mock(ModuleFormService.class);
        ApplicationContext context = mock(ApplicationContext.class);
        CustomFormDataFieldService service = new CustomFormDataFieldService();
        ReflectionTestUtils.setField(service, "fieldReferenceService", new FieldReferenceService());
        ReflectionTestUtils.setField(service, "customFormDataFieldMapper", fieldMapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldBlobMapper", blobMapper);

        when(context.getBean(ModuleFormService.class)).thenReturn(moduleFormService);
        new CommonBeanFactory().setApplicationContext(context);
        OrganizationContext.setOrganizationId("org-1");
        CustomFormDataFieldService.setFormKey("form-1");

        CustomFormDataField persisted = new CustomFormDataField();
        persisted.setResourceId("record-1");
        persisted.setFieldId("number-field");
        persisted.setFieldValue("not-a-number");
        when(fieldMapper.selectListByLambda(any())).thenReturn(List.of(persisted));
        when(blobMapper.selectListByLambda(any())).thenReturn(List.of());

        when(moduleFormService.getFlattenFormFields("form-1", "org-1"))
                .thenReturn(List.of());
        assertEquals(List.of(), service.getModuleFieldValuesByResourceId("record-1"));
        assertEquals(0, service.getResourceFieldMap(List.of("record-1"), true).size());

        InputNumberField numberField = new InputNumberField();
        numberField.setId("number-field");
        numberField.setType("INPUT_NUMBER");
        when(moduleFormService.getFlattenFormFields("form-1", "org-1"))
                .thenReturn(List.of(numberField));
        assertEquals(List.of(), service.getModuleFieldValuesByResourceId("record-1"));

        when(fieldMapper.selectListByLambda(any())).thenReturn(List.of());
        when(blobMapper.selectListByLambda(any()))
                .thenThrow(new IllegalStateException("blob read failed"));
        assertEquals(List.of(), service.getModuleFieldValuesByResourceId("record-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptySubtableRowIsPersistedAndSparseRowsRemainReadable() {
        BaseMapper<CustomFormDataField> fieldMapper = mock(BaseMapper.class);
        BaseMapper<CustomFormDataFieldBlob> blobMapper = mock(BaseMapper.class);
        ModuleFormService moduleFormService = mock(ModuleFormService.class);
        DefaultUidGenerator uidGenerator = new DefaultUidGenerator();
        ReflectionTestUtils.setField(uidGenerator, "workerIdAssigner", (WorkerIdAssigner) () -> 1L);
        uidGenerator.init();
        ApplicationContext context = mock(ApplicationContext.class);
        CustomFormDataFieldService service = new CustomFormDataFieldService();
        ReflectionTestUtils.setField(service, "fieldReferenceService", new FieldReferenceService());
        ReflectionTestUtils.setField(service, "customFormDataFieldMapper", fieldMapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldBlobMapper", blobMapper);

        when(context.getBean(ModuleFormService.class)).thenReturn(moduleFormService);
        when(context.getBean(DefaultUidGenerator.class)).thenReturn(uidGenerator);
        new CommonBeanFactory().setApplicationContext(context);
        OrganizationContext.setOrganizationId("org-1");
        CustomFormDataFieldService.setFormKey("form-1");

        InputField childField = new InputField();
        childField.setId("child-field");
        childField.setType("INPUT");
        ProductSubField subtable = new ProductSubField();
        subtable.setId("subtable-field");
        subtable.setType("SUB_PRODUCT");
        subtable.setSubFields(List.of(childField));

        List<Map<String, Object>> rows = List.of(
                Map.of("id", "row-1"),
                Map.of("id", "row-2", "child-field", "保留值"));
        List<CustomFormDataField> savedFields = new ArrayList<>();
        service.saveSubFieldValue(
                "record-1",
                subtable,
                new BaseModuleFieldValue("subtable-field", rows),
                savedFields,
                new ArrayList<>());

        assertEquals(2, savedFields.size());
        assertEquals("id", savedFields.get(0).getFieldId());
        assertEquals("1", savedFields.get(0).getRowId());
        assertEquals("row-1", savedFields.get(0).getBizId());
        assertEquals("2", savedFields.get(1).getRowId());

        when(fieldMapper.selectListByLambda(any())).thenReturn(List.of(savedFields.get(1)));
        when(blobMapper.selectListByLambda(any())).thenReturn(List.of());
        when(moduleFormService.getFlattenFormFields("form-1", "org-1"))
                .thenReturn(List.of(subtable, childField));

        List<BaseModuleFieldValue> values = service.getModuleFieldValuesByResourceId("record-1");
        assertEquals(1, values.size());
        List<Map<String, Object>> restoredRows = (List<Map<String, Object>>) values.get(0).getFieldValue();
        assertEquals(2, restoredRows.size());
        assertNotNull(restoredRows.get(0).get("id"));
        assertEquals("row-2", restoredRows.get(1).get("id"));
        assertEquals("保留值", restoredRows.get(1).get("child-field"));

        // 还原上下文后，静态缓存的生成器仍须持续生成唯一 ID。
        new CommonBeanFactory().setApplicationContext(previousContext);
        assertNotEquals(IDGenerator.nextStr(), IDGenerator.nextStr());
    }
}
