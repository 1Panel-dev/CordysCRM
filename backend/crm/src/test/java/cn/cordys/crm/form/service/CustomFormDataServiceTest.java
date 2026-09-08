package cn.cordys.crm.form.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.formula.FormulaRequestCompletionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.dto.request.CustomFormDataAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataUpdateRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomFormDataServiceTest {

    @BeforeAll
    static void initializeTranslator() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/cordys-crm");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        new Translator().setMessageSource(messageSource);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void acceptsUpdateWhenRequestFormOwnsRecord() {
        assertDoesNotThrow(
                () -> CustomFormDataService.validateCustomFormId("form-1", "form-1"));
    }

    @Test
    void rejectsUpdateWhenRequestFormDoesNotOwnRecord() {
        GenericException error = assertThrows(GenericException.class,
                () -> CustomFormDataService.validateCustomFormId("form-1", "form-2"));

        assertEquals(CrmHttpResultCode.VALIDATE_FAILED, error.getErrorCode());
    }

    @Test
    void returnsEnglishValidationMessagesForEnglishLocale() {
        LocaleContextHolder.setLocale(Locale.US);

        assertEquals("The custom form ID does not match the record's actual form",
                assertThrows(GenericException.class,
                        () -> CustomFormDataService.validateCustomFormId("form-1", "form-2"))
                        .getMessage());
        assertEquals("Custom field ID cannot be blank",
                assertThrows(GenericException.class,
                        () -> CustomFormDataService.mergeModuleFields(
                                List.of(), List.of(new BaseModuleFieldValue(null, "value"))))
                        .getMessage());
        assertEquals("Name cannot be blank",
                assertThrows(GenericException.class,
                        () -> CustomFormDataService.validateCalculatedName(" "))
                        .getMessage());
        assertEquals("Name length cannot exceed 255 characters",
                assertThrows(GenericException.class,
                        () -> CustomFormDataService.validateCalculatedName("a".repeat(256)))
                        .getMessage());
    }

    @Test
    void returnsChineseValidationMessagesForChineseLocale() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        assertEquals("自定义表单ID与记录实际归属不一致",
                assertThrows(GenericException.class,
                        () -> CustomFormDataService.validateCustomFormId("form-1", "form-2"))
                        .getMessage());
        assertEquals("自定义字段ID不能为空",
                assertThrows(GenericException.class,
                        () -> CustomFormDataService.mergeModuleFields(
                                List.of(), List.of(new BaseModuleFieldValue(null, "value"))))
                        .getMessage());
        assertEquals("名称不能为空",
                assertThrows(GenericException.class,
                        () -> CustomFormDataService.validateCalculatedName(" "))
                        .getMessage());
        assertEquals("名称长度不能超过255个字符",
                assertThrows(GenericException.class,
                        () -> CustomFormDataService.validateCalculatedName("a".repeat(256)))
                        .getMessage());
    }

    @Test
    void rejectsCrossFormUpdateBeforeAnyRecordWrite() {
        @SuppressWarnings("unchecked")
        BaseMapper<CustomFormData> mapper = mock(BaseMapper.class);
        CustomFormData origin = new CustomFormData();
        origin.setId("data-1");
        origin.setCustomFormId("form-1");
        when(mapper.selectByPrimaryKey("data-1")).thenReturn(origin);
        CustomFormDataService service = new CustomFormDataService();
        ReflectionTestUtils.setField(service, "customFormDataMapper", mapper);
        CustomFormDataUpdateRequest request = new CustomFormDataUpdateRequest();
        request.setId("data-1");
        request.setCustomFormId("form-2");
        request.setName("renamed");

        assertThrows(GenericException.class,
                () -> service.update(request, "user-1", "org-1"));

        verify(mapper, never()).update(any(CustomFormData.class));
    }

    @Test
    void mergesOldValuesAndCalculatesFormulaAfterPermissionBeforeUpdate() {
        @SuppressWarnings("unchecked")
        BaseMapper<CustomFormData> mapper = mock(BaseMapper.class);
        CustomFormDataFieldService fieldService = mock(CustomFormDataFieldService.class);
        FormulaRequestCompletionService formulaService =
                mock(FormulaRequestCompletionService.class);
        BaseService baseService = mock(BaseService.class);
        ModuleFormCacheService cacheService = mock(ModuleFormCacheService.class);
        CustomFormData origin = customFormData();
        when(mapper.selectByPrimaryKey("data-1")).thenReturn(origin);
        when(fieldService.getModuleFieldValuesByResourceIdStrict("data-1"))
                .thenReturn(List.of(
                        new BaseModuleFieldValue("amount", 5),
                        new BaseModuleFieldValue("total", 10)));
        when(cacheService.getBusinessFormConfig("form-1", "org-1"))
                .thenReturn(new ModuleFormConfigDTO());

        CustomFormDataService service = spy(new CustomFormDataService());
        doReturn(CustomFormRoleKey.MANAGE_ALL)
                .when(service).getManageDataScope("form-1", "user-1");
        ReflectionTestUtils.setField(service, "customFormDataMapper", mapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldService", fieldService);
        ReflectionTestUtils.setField(service, "formulaRequestCompletionService", formulaService);
        ReflectionTestUtils.setField(service, "baseService", baseService);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", cacheService);

        doAnswer(invocation -> {
            CustomFormDataUpdateRequest calculating = invocation.getArgument(1);
            Map<String, BaseModuleFieldValue> values = calculating.getModuleFields().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            BaseModuleFieldValue::getFieldId, value -> value));
            assertEquals(7, values.get("amount").getFieldValue());
            assertEquals(10, values.get("total").getFieldValue());
            values.get("total").setFieldValue(14);
            calculating.setName("N-7");
            return null;
        }).when(formulaService).completeAuthoritative(
                eq("form-1"), any(CustomFormDataUpdateRequest.class), eq(false));

        CustomFormDataUpdateRequest request = new CustomFormDataUpdateRequest();
        request.setId("data-1");
        request.setCustomFormId("form-1");
        request.setModuleFields(List.of(new BaseModuleFieldValue("amount", 7)));

        service.update(request, "user-1", "org-1");

        ArgumentCaptor<CustomFormData> dataCaptor =
                ArgumentCaptor.forClass(CustomFormData.class);
        verify(mapper).update(dataCaptor.capture());
        assertEquals("N-7", dataCaptor.getValue().getName());
        assertEquals("owner-1", dataCaptor.getValue().getOwner());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BaseModuleFieldValue>> fieldCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(fieldService).saveModuleField(
                any(CustomFormData.class), eq("org-1"), eq("user-1"),
                fieldCaptor.capture(), eq(true));
        Map<String, Object> saved = fieldCaptor.getValue().stream()
                .collect(java.util.stream.Collectors.toMap(
                        BaseModuleFieldValue::getFieldId,
                        BaseModuleFieldValue::getFieldValue));
        assertEquals(Map.of("amount", 7, "total", 14), saved);
    }

    @Test
    void formulaFailureStopsUpdateBeforeOldValuesAreDeleted() {
        @SuppressWarnings("unchecked")
        BaseMapper<CustomFormData> mapper = mock(BaseMapper.class);
        CustomFormDataFieldService fieldService = mock(CustomFormDataFieldService.class);
        FormulaRequestCompletionService formulaService =
                mock(FormulaRequestCompletionService.class);
        when(mapper.selectByPrimaryKey("data-1"))
                .thenReturn(customFormData());
        when(fieldService.getModuleFieldValuesByResourceIdStrict("data-1"))
                .thenReturn(List.of(new BaseModuleFieldValue("amount", 5)));
        doAnswer(ignored -> {
            throw new IllegalArgumentException("公式循环依赖");
        }).when(formulaService).completeAuthoritative(
                eq("form-1"), any(CustomFormDataUpdateRequest.class), eq(false));

        CustomFormDataService service = spy(new CustomFormDataService());
        doReturn(CustomFormRoleKey.MANAGE_ALL)
                .when(service).getManageDataScope("form-1", "user-1");
        ReflectionTestUtils.setField(service, "customFormDataMapper", mapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldService", fieldService);
        ReflectionTestUtils.setField(service, "formulaRequestCompletionService", formulaService);

        CustomFormDataUpdateRequest request = new CustomFormDataUpdateRequest();
        request.setId("data-1");
        request.setCustomFormId("form-1");
        request.setModuleFields(List.of(new BaseModuleFieldValue("amount", 7)));

        assertThrows(IllegalArgumentException.class,
                () -> service.update(request, "user-1", "org-1"));

        verify(mapper, never()).update(any(CustomFormData.class));
        verify(fieldService, never()).deleteByResourceId("data-1");
        verify(fieldService, never()).saveModuleField(
                any(), eq("org-1"), eq("user-1"), anyList(), eq(true));
    }

    @Test
    void emptyCalculatedNameStopsCreateBeforeAnyInsert() {
        @SuppressWarnings("unchecked")
        BaseMapper<CustomFormData> mapper = mock(BaseMapper.class);
        FormulaRequestCompletionService formulaService =
                mock(FormulaRequestCompletionService.class);
        CustomFormDataService service = spy(new CustomFormDataService());
        doReturn(CustomFormRoleKey.MANAGE_ALL)
                .when(service).getManageDataScope("form-1", "user-1");
        ReflectionTestUtils.setField(service, "customFormDataMapper", mapper);
        ReflectionTestUtils.setField(service, "formulaRequestCompletionService", formulaService);

        CustomFormDataAddRequest request = new CustomFormDataAddRequest();
        request.setCustomFormId("form-1");

        assertThrows(GenericException.class,
                () -> service.add(request, "user-1", "org-1"));

        verify(mapper, never()).insert(any(CustomFormData.class));
    }

    private static CustomFormData customFormData() {
        CustomFormData data = new CustomFormData();
        data.setId("data-1");
        data.setCustomFormId("form-1");
        data.setName("N-5");
        data.setOwner("owner-1");
        data.setCreateUser("user-1");
        return data;
    }
}
