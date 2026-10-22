package cn.cordys.crm.form.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.service.FieldReferenceService;
import cn.cordys.common.service.FieldSourceServiceProvider;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CustomFormReferenceContextTest {

    private final CustomFormDataService dataService = new CustomFormDataService();
    private final CustomFormDataFieldService fields = mock(CustomFormDataFieldService.class);
    private final CustomFormDataFieldService hostFields = spy(new CustomFormDataFieldService());
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomFormData> dataMapper = mock(BaseMapper.class);
    private final List<BaseModuleFieldValue> values = List.of(new BaseModuleFieldValue("score", 42));

    @BeforeEach
    void setUp() {
        CustomFormDataFieldService.clearFormKey();
        ReflectionTestUtils.setField(dataService, "customFormDataMapper", dataMapper);
        ReflectionTestUtils.setField(dataService, "customFormDataFieldService", fields);
        ReflectionTestUtils.setField(hostFields, "fieldReferenceService", new FieldReferenceService());
        CustomFormData record = new CustomFormData();
        record.setId("child-record");
        record.setCustomFormId("record-form");
        when(dataMapper.selectByPrimaryKey("child-record")).thenReturn(record);
        when(fields.getModuleFieldValuesByResourceId("child-record")).thenAnswer(invocation -> {
            assertEquals("record-form", hostFields.getFormKey());
            return values;
        });
    }

    @AfterEach
    void clearFormContext() {
        CustomFormDataFieldService.clearFormKey();
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void simpleDetailRestoresThePreviousContextOnSuccessAndFailure(boolean hasParent, boolean fail) {
        if (hasParent) {
            CustomFormDataFieldService.setFormKey("parent-form");
        }
        if (fail) {
            IllegalStateException failure = failFieldRead();
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> dataService.getSimple("child-record")));
        } else {
            assertSame(values, dataService.getSimple("child-record").getModuleFields());
        }

        if (hasParent) {
            assertEquals("parent-form", hostFields.getFormKey());
        } else {
            assertThrows(IllegalStateException.class, hostFields::getFormKey);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void snapshotReferenceKeepsTheOuterFormForSubsequentFields(boolean subTable) {
        ModuleFormService forms = snapshotService();
        CustomFormDataFieldService.setFormKey("parent-form");
        doAnswer(invocation -> {
            assertEquals("parent-form", hostFields.getFormKey());
            return "P-001";
        }).when(hostFields).getResourceFieldValue("parent-record", "serial");
        Map<String, Object> row = new HashMap<>(Map.of("source", "child-record"));

        List<BaseModuleFieldValue> result = forms.resolveSnapshotFields(snapshotValues(subTable, row),
                snapshotForm(subTable), hostFields, "parent-record");

        if (subTable) {
            assertEquals(42, row.get("source_ref_score"));
        } else {
            assertTrue(result.contains(new BaseModuleFieldValue("source_ref_score", 42)));
        }
        assertTrue(result.contains(new BaseModuleFieldValue("serial", "P-001")));
        assertEquals("parent-form", hostFields.getFormKey());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedSnapshotReferenceRestoresTheOuterForm(boolean subTable) {
        ModuleFormService forms = snapshotService();
        CustomFormDataFieldService.setFormKey("parent-form");
        IllegalStateException failure = failFieldRead();

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> forms.resolveSnapshotFields(
                        snapshotValues(subTable, new HashMap<>(Map.of("source", "child-record"))),
                        snapshotForm(subTable), hostFields, "parent-record")));

        assertEquals("parent-form", hostFields.getFormKey());
    }

    private ModuleFormService snapshotService() {
        ModuleFormService forms = new ModuleFormService();
        FieldSourceServiceProvider sources = mock(FieldSourceServiceProvider.class);
        ReflectionTestUtils.setField(forms, "fieldSourceServiceProvider", sources);
        when(sources.safeGetSimpleById(FieldSourceType.CUSTOM_FORM, "child-record")).thenAnswer(invocation -> {
            assertEquals("configured-form", hostFields.getFormKey());
            try {
                return dataService.getSimple("child-record");
            } finally {
                assertEquals("configured-form", hostFields.getFormKey());
            }
        });
        return forms;
    }

    private IllegalStateException failFieldRead() {
        IllegalStateException failure = new IllegalStateException("field read failed");
        doAnswer(invocation -> {
            assertEquals("record-form", hostFields.getFormKey());
            throw failure;
        }).when(fields).getModuleFieldValuesByResourceId("child-record");
        return failure;
    }

    private List<BaseModuleFieldValue> snapshotValues(boolean subTable, Map<String, Object> row) {
        return new ArrayList<>(List.of(subTable
                ? new BaseModuleFieldValue("products", List.of(row))
                : new BaseModuleFieldValue("source", "child-record")));
    }

    private ModuleFormConfigDTO snapshotForm(boolean subTable) {
        DatasourceField source = new DatasourceField();
        source.setId("source");
        source.setDataSourceType("configured-form");
        source.setShowFields(List.of("score"));
        InputField reference = new InputField();
        reference.setId("source_ref_score");
        reference.setResourceFieldId("source");
        SerialNumberField serial = new SerialNumberField();
        serial.setId("serial");
        serial.setType(FieldType.SERIAL_NUMBER.name());
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        if (subTable) {
            SubField table = new SubField();
            table.setId("table-id");
            table.setBusinessKey("products");
            table.setType(FieldType.SUB_PRODUCT.name());
            table.setSubFields(List.of(source, reference));
            config.setFields(new ArrayList<>(List.of(table, serial)));
        } else {
            config.setFields(new ArrayList<>(List.of(source, reference, serial)));
        }
        return config;
    }
}
