package cn.cordys.common.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.domain.CustomFormDataField;
import cn.cordys.crm.form.domain.CustomFormDataFieldBlob;
import cn.cordys.crm.form.service.CustomFormDataFieldService;
import cn.cordys.crm.system.dto.field.InputNumberField;
import cn.cordys.crm.system.dto.field.TextAreaField;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseResourceFieldServiceTest {
    private ApplicationContext previousContext;

    @BeforeEach
    void saveContext() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
    }

    @AfterEach
    void cleanContext() {
        CustomFormDataFieldService.clearFormKey();
        OrganizationContext.clear();
        new CommonBeanFactory().setApplicationContext(previousContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateExistingBlobFieldUpdatesInsteadOfInsertingDuplicate() {
        BaseMapper<CustomFormDataField> fieldMapper = mock(BaseMapper.class);
        BaseMapper<CustomFormDataFieldBlob> blobMapper = mock(BaseMapper.class);
        ModuleFormService moduleFormService = mock(ModuleFormService.class);
        ApplicationContext context = mock(ApplicationContext.class);
        CustomFormDataFieldService service = new CustomFormDataFieldService();
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

        ArgumentCaptor<CustomFormDataFieldBlob> updated =
                ArgumentCaptor.forClass(CustomFormDataFieldBlob.class);
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
}
