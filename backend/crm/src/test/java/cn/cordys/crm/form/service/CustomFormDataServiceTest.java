package cn.cordys.crm.form.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.dto.request.CustomFormDataBatchUpdateRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataUpdateRequest;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.TextAreaField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomFormDataServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void batchUpdateKeepsOriginalBatchPathWithoutFormulaCalculation() {
        BaseMapper<CustomFormData> mapper = mock(BaseMapper.class);
        CustomFormDataFieldService fields = mock(CustomFormDataFieldService.class);
        CustomFormDataService service = org.mockito.Mockito.spy(serviceWithManageAllPermission());
        ReflectionTestUtils.setField(service, "customFormDataMapper", mapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldService", fields);
        ReflectionTestUtils.setField(service, "extCustomFormDataMapper",
                mock(cn.cordys.crm.form.mapper.ExtCustomFormDataMapper.class));
        CustomFormData first = new CustomFormData();
        first.setId("one");
        first.setCustomFormId("form-1");
        first.setOrganizationId("org-1");
        CustomFormData second = new CustomFormData();
        second.setId("two");
        second.setCustomFormId("form-1");
        second.setOrganizationId("org-1");
        when(mapper.selectByIds(anyList())).thenReturn(List.of(first, second));
        InputField field = new InputField();
        field.setId("note");
        field.setType("INPUT");
        when(fields.getAndCheckField("note", "org-1")).thenReturn(field);
        java.util.List<CustomFormDataUpdateRequest> updates = new java.util.ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            updates.add(invocation.getArgument(0));
            return null;
        }).when(service).update(org.mockito.ArgumentMatchers.any(), anyString(), anyString());
        CustomFormDataBatchUpdateRequest request = new CustomFormDataBatchUpdateRequest();
        request.setCustomFormId("form-1");
        request.setIds(List.of("one", "two"));
        request.setFieldId("note");
        request.setFieldValue("new");

        service.batchUpdate(request, "user-1", "org-1");

        assertEquals(List.of(), updates);
        verify(fields).batchUpdate(org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.eq(field),
                org.mockito.ArgumentMatchers.eq(List.of(first, second)), org.mockito.ArgumentMatchers.eq(CustomFormData.class),
                anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("org-1"));
    }

    private MessageSource originalMessageSource;

    @BeforeEach
    void initializeTranslator() {
        originalMessageSource = (MessageSource) ReflectionTestUtils.getField(
                Translator.class, "messageSource");
        ReflectionTestUtils.setField(
                Translator.class, "messageSource", new StaticMessageSource());
    }

    @AfterEach
    void restoreTranslator() {
        ReflectionTestUtils.setField(
                Translator.class, "messageSource", originalMessageSource);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateKeepsReplacementContractAfterOwnerTransferWithoutFormulaCalculation() {
        BaseMapper<CustomFormData> dataMapper = mock(BaseMapper.class);
        CustomFormDataFieldService fieldService = mock(
                CustomFormDataFieldService.class);
        ModuleFormCacheService formCacheService = mock(
                ModuleFormCacheService.class);
        BaseService baseService = mock(BaseService.class);
        CustomFormDataService service = serviceWithPermission(CustomFormRoleKey.MANAGE_OWN);
        ReflectionTestUtils.setField(service, "customFormDataMapper", dataMapper);
        ReflectionTestUtils.setField(
                service, "customFormDataFieldService", fieldService);
        ReflectionTestUtils.setField(
                service, "moduleFormCacheService", formCacheService);
        ReflectionTestUtils.setField(service, "baseService", baseService);

        CustomFormData persisted = new CustomFormData();
        persisted.setId("record-1");
        persisted.setCustomFormId("form-1");
        persisted.setName("原名称");
        persisted.setOwner("user-1");
        persisted.setCreateUser("previous-owner");
        persisted.setOrganizationId("org-1");
        when(dataMapper.selectByPrimaryKey("record-1")).thenReturn(persisted);

        SerialNumberField serial = new SerialNumberField();
        serial.setId("serial-field");
        serial.setType("SERIAL_NUMBER");
        InputField note = new InputField();
        note.setId("note-field");
        note.setType("INPUT");
        TextAreaField description = new TextAreaField();
        description.setId("description-field");
        description.setType("TEXTAREA");
        SubField keptItems = new SubField();
        keptItems.setId("kept-items-field");
        keptItems.setType("SUB_PRODUCT");
        SubField clearedItems = new SubField();
        clearedItems.setId("cleared-items-field");
        clearedItems.setType("SUB_PRODUCT");
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(List.of(serial, note, description, keptItems, clearedItems));
        when(formCacheService.getBusinessFormConfig("form-1", "org-1"))
                .thenReturn(config);
        when(fieldService.getModuleFieldValuesByResourceId("record-1"))
                .thenReturn(List.of(
                        new BaseModuleFieldValue("serial-field", "SN-0001"),
                        new BaseModuleFieldValue("note-field", "旧备注"),
                        new BaseModuleFieldValue("description-field", "旧长文本"),
                        new BaseModuleFieldValue("kept-items-field", List.of(Map.of("id", "row-1"))),
                        new BaseModuleFieldValue("cleared-items-field", List.of(Map.of("id", "row-2")))));

        CustomFormDataUpdateRequest request = new CustomFormDataUpdateRequest();
        request.setId("record-1");
        request.setCustomFormId("form-1");
        request.setName("新名称");
        request.setModuleFields(List.of(
                new BaseModuleFieldValue("serial-field", "伪造编号"),
                new BaseModuleFieldValue("note-field", "新备注"),
                new BaseModuleFieldValue("cleared-items-field", List.of())));

        service.update(request, "user-1", "org-1");

        assertEquals(3, request.getModuleFields().size());
        assertEquals("新备注", valueOf(request, "note-field"));
        assertEquals("伪造编号", valueOf(request, "serial-field"));
        assertEquals(List.of(), valueOf(request, "cleared-items-field"));
        verify(fieldService).saveModuleField(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("org-1"),
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq(request.getModuleFields()),
                org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchUpdateRejectsWhenAnyRequestedRecordIsMissing() {
        BaseMapper<CustomFormData> dataMapper = mock(BaseMapper.class);
        CustomFormDataService service = serviceWithManageAllPermission();
        ReflectionTestUtils.setField(service, "customFormDataMapper", dataMapper);
        CustomFormData existing = new CustomFormData();
        existing.setId("record-1");
        when(dataMapper.selectByIds(anyList())).thenReturn(List.of(existing));

        CustomFormDataBatchUpdateRequest request =
                new CustomFormDataBatchUpdateRequest();
        request.setCustomFormId("form-1");
        request.setIds(List.of("record-1", "missing-record"));
        request.setFieldId("note-field");
        request.setFieldValue("新备注");

        assertThrows(GenericException.class,
                () -> service.batchUpdate(request, "user-1", "org-1"));
        verify(dataMapper, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteAllowsManageOwnUserAfterOwnerTransfer() {
        BaseMapper<CustomFormData> dataMapper = mock(BaseMapper.class);
        CustomFormDataFieldService fieldService = mock(CustomFormDataFieldService.class);
        CustomFormDataService service = serviceWithPermission(CustomFormRoleKey.MANAGE_OWN);
        ReflectionTestUtils.setField(service, "customFormDataMapper", dataMapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldService", fieldService);

        CustomFormData transferred = record("record-1", "form-1", "org-1", "owner-2");
        transferred.setCreateUser("owner-1");
        when(dataMapper.selectByPrimaryKey("record-1")).thenReturn(transferred);

        service.delete("record-1", "owner-2", "org-1");

        verify(fieldService).deleteByResourceId("record-1");
        verify(dataMapper).deleteByPrimaryKey("record-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRejectsPreviousCreatorAfterOwnerTransfer() {
        BaseMapper<CustomFormData> dataMapper = mock(BaseMapper.class);
        CustomFormDataService service = serviceWithPermission(CustomFormRoleKey.MANAGE_OWN);
        ReflectionTestUtils.setField(service, "customFormDataMapper", dataMapper);

        CustomFormData transferred = record("record-1", "form-1", "org-1", "owner-2");
        transferred.setCreateUser("owner-1");
        when(dataMapper.selectByPrimaryKey("record-1")).thenReturn(transferred);

        assertThrows(GenericException.class,
                () -> service.get("record-1", "owner-1", "org-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchUpdateRejectsRecordFromOtherOrganization() {
        BaseMapper<CustomFormData> dataMapper = mock(BaseMapper.class);
        CustomFormDataFieldService fieldService = mock(CustomFormDataFieldService.class);
        CustomFormDataService service = serviceWithManageAllPermission();
        ReflectionTestUtils.setField(service, "customFormDataMapper", dataMapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldService", fieldService);
        when(dataMapper.selectByIds(anyList()))
                .thenReturn(List.of(record("record-1", "form-1", "org-2", "owner-1")));

        CustomFormDataBatchUpdateRequest request = new CustomFormDataBatchUpdateRequest();
        request.setCustomFormId("form-1");
        request.setIds(List.of("record-1"));
        request.setFieldId("note-field");
        request.setFieldValue("新备注");

        assertThrows(GenericException.class,
                () -> service.batchUpdate(request, "user-1", "org-1"));
        verify(fieldService, never()).getAndCheckField(anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateRejectsRecordFromOtherOrganizationBeforeReadingFields() {
        BaseMapper<CustomFormData> dataMapper = mock(BaseMapper.class);
        CustomFormDataFieldService fieldService = mock(CustomFormDataFieldService.class);
        CustomFormDataService service = serviceWithManageAllPermission();
        ReflectionTestUtils.setField(service, "customFormDataMapper", dataMapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldService", fieldService);
        when(dataMapper.selectByPrimaryKey("record-1"))
                .thenReturn(record("record-1", "form-1", "org-2", "owner-1"));

        CustomFormDataUpdateRequest request = new CustomFormDataUpdateRequest();
        request.setId("record-1");
        request.setCustomFormId("form-1");
        request.setName("新名称");

        assertThrows(GenericException.class,
                () -> service.update(request, "user-1", "org-1"));
        verify(fieldService, never()).getModuleFieldValuesByResourceId(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateWithoutModuleFieldsDoesNotReadOrRewriteThemAndReadFailureStopsReplacement() {
        BaseMapper<CustomFormData> dataMapper = mock(BaseMapper.class);
        CustomFormDataFieldService fieldService = mock(CustomFormDataFieldService.class);
        ModuleFormCacheService formCacheService = mock(ModuleFormCacheService.class);
        CustomFormDataService service = serviceWithManageAllPermission();
        ReflectionTestUtils.setField(service, "customFormDataMapper", dataMapper);
        ReflectionTestUtils.setField(service, "customFormDataFieldService", fieldService);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", formCacheService);

        CustomFormData persisted = record("record-1", "form-1", "org-1", "owner-1");
        persisted.setName("原名称");
        when(dataMapper.selectByPrimaryKey("record-1")).thenReturn(persisted);
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(List.of());
        when(formCacheService.getBusinessFormConfig("form-1", "org-1"))
                .thenReturn(config);
        when(fieldService.getModuleFieldValuesByResourceId("record-1"))
                .thenThrow(new GenericException("严格读取失败"));
        ReflectionTestUtils.setField(service, "baseService", mock(BaseService.class));

        CustomFormDataUpdateRequest request = new CustomFormDataUpdateRequest();
        request.setId("record-1");
        request.setCustomFormId("form-1");
        request.setName("新名称");

        service.update(request, "user-1", "org-1");
        verify(fieldService, never()).getModuleFieldValuesByResourceId(anyString());
        org.junit.jupiter.api.Assertions.assertNull(request.getModuleFields());
        request.setModuleFields(List.of(new BaseModuleFieldValue("note", "新备注")));
        assertThrows(GenericException.class,
                () -> service.update(request, "user-1", "org-1"));
        verify(fieldService, never()).deleteByResourceId(anyString());
        verify(fieldService, never()).saveModuleField(
                org.mockito.ArgumentMatchers.any(), anyString(), anyString(), anyList(), anyBoolean());
    }

    @Test
    @SuppressWarnings("unchecked")
    void customFormDeleteRejectsOtherOrganizationBeforeAdminCheck() {
        BaseMapper<CustomForm> formMapper = mock(BaseMapper.class);
        ModuleFormCacheService formCacheService = mock(ModuleFormCacheService.class);
        CustomFormService service = new CustomFormService();
        ReflectionTestUtils.setField(service, "customFormMapper", formMapper);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", formCacheService);

        CustomForm form = new CustomForm();
        form.setId("form-1");
        form.setOrganizationId("org-2");
        when(formMapper.selectByPrimaryKey("form-1")).thenReturn(form);

        assertThrows(GenericException.class,
                () -> service.delete("form-1", "user-1", "org-1"));
        verify(formCacheService, never()).delete(anyString(), anyString());
    }

    private Object valueOf(
            CustomFormDataUpdateRequest request, String fieldId) {
        return request.getModuleFields().stream()
                .filter(value -> fieldId.equals(value.getFieldId()))
                .findFirst()
                .orElseThrow()
                .getFieldValue();
    }

    private CustomFormDataService serviceWithManageAllPermission() {
        return serviceWithPermission(CustomFormRoleKey.MANAGE_ALL);
    }

    private CustomFormData record(String id, String formId, String orgId, String owner) {
        CustomFormData data = new CustomFormData();
        data.setId(id);
        data.setCustomFormId(formId);
        data.setOrganizationId(orgId);
        data.setOwner(owner);
        return data;
    }

    private CustomFormDataService serviceWithPermission(CustomFormRoleKey permission) {
        return new CustomFormDataService() {
            @Override
            CustomFormRoleKey getManageDataScope(String formId, String userId, String orgId) {
                return permission;
            }

            @Override
            CustomFormRoleKey getDataScope(String formId, String userId, String orgId) {
                return permission;
            }
        };
    }
}
