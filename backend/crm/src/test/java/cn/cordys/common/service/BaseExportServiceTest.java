package cn.cordys.common.service;

import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.FieldExportMeta;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.SelectMultipleResolver;
import cn.cordys.common.resolver.field.SelectResolver;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.SelectMultipleField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseExportServiceTest {

    private final BaseExportService exportService = new BaseExportService() {
    };

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void selectedExportIncludesOnlyRoleOrCollaborationVisibleRows(boolean roleCanRead) {
        DeptDataPermissionDTO roleScope = new DeptDataPermissionDTO();
        roleScope.setSelf(roleCanRead);
        ExportDTO export = ExportDTO.builder().deptDataPermission(roleScope)
                .selectIds(List.of("collaborative", "role-owned", "inaccessible", "collaborative")).build();
        BasePageRequest request = new BasePageRequest();

        List<String> result = exportService.loadSelectedVisibleRows(export, request,
                InternalUserView.CUSTOMER_COLLABORATION, scope -> {
                    if (scope == roleScope) {
                        assertTrue(roleCanRead);
                        assertEquals(List.of("collaborative", "role-owned", "inaccessible"), request.getExportIds());
                        return List.of("role-owned");
                    }
                    assertTrue(scope.getVisible());
                    assertEquals(InternalUserView.CUSTOMER_COLLABORATION.name(), scope.getViewId());
                    assertEquals(roleCanRead ? List.of("collaborative", "inaccessible")
                            : List.of("collaborative", "role-owned", "inaccessible"), request.getExportIds());
                    return List.of("collaborative", "collaborative", "unselected");
                }, Function.identity());

        assertEquals(roleCanRead ? List.of("collaborative", "role-owned") : List.of("collaborative"), result);
    }

    @Test
    void shouldConvertSystemOptionCodesToLabels() {
        SelectField typeField = selectField("recordType", "type", "CUSTOMER", "客户", "CLUE", "线索");
        SelectField methodField = selectField("recordMethod", "method", "1", "到访", "2", "电话");

        LinkedHashMap<String, Object> systemFields = new LinkedHashMap<>();
        systemFields.put("type", "CUSTOMER");
        systemFields.put("method", "1");

        List<Object> values = exportService.transFieldValueWithSub(
                List.of(meta(typeField, new SelectResolver()), meta(methodField, new SelectResolver())),
                systemFields, Map.of(), Map.of(), new HashMap<>());

        assertEquals(List.of("客户", "到访"), values);
    }

    @Test
    void shouldNotTransformValueThatIsAlreadyOptionLabel() {
        SelectField typeField = selectField("recordType", "type", "CUSTOMER", "客户", "CLUE", "线索");
        LinkedHashMap<String, Object> systemFields = new LinkedHashMap<>();
        systemFields.put("type", "客户");

        List<Object> values = exportService.transFieldValueWithSub(List.of(meta(typeField, new SelectResolver())),
                systemFields, Map.of(), Map.of(), new HashMap<>());

        assertEquals(List.of("客户"), values);
    }

    @Test
    void shouldIsolateOptionCacheByField() {
        SelectField firstField = selectField("firstOption", "first", "1", "选项一");
        SelectField secondField = selectField("secondOption", "second", "1", "选项二");
        LinkedHashMap<String, Object> systemFields = new LinkedHashMap<>();
        systemFields.put("first", "1");
        systemFields.put("second", "1");
        Map<Object, Object> cache = new HashMap<>();

        List<Object> values = exportService.transFieldValueWithSub(
                List.of(meta(firstField, new SelectResolver()), meta(secondField, new SelectResolver())),
                systemFields, Map.of(), Map.of(), cache);

        assertEquals(List.of("选项一", "选项二"), values);
    }

    @Test
    void shouldConvertMultipleOptionValuesToLabels() {
        SelectMultipleField field = new SelectMultipleField();
        field.setId("tags");
        field.setBusinessKey("tags");
        field.setType("SELECT_MULTIPLE");
        field.setOptions(List.of(new OptionProp("A", "甲"), new OptionProp("B", "乙")));
        LinkedHashMap<String, Object> systemFields = new LinkedHashMap<>();
        systemFields.put("tags", "[\"A\",\"B\"]");

        List<Object> values = exportService.transFieldValueWithSub(List.of(meta(field, new SelectMultipleResolver())),
                systemFields, Map.of(), Map.of(), new HashMap<>());

        assertEquals(List.of("甲,乙"), values);
    }

    @Test
    void shouldKeepUnknownOptionValueUnchanged() {
        SelectField typeField = selectField("recordType", "type", "CUSTOMER", "客户");
        LinkedHashMap<String, Object> systemFields = new LinkedHashMap<>();
        systemFields.put("type", "UNKNOWN");

        List<Object> values = exportService.transFieldValueWithSub(List.of(meta(typeField, new SelectResolver())),
                systemFields, Map.of(), Map.of(), new HashMap<>());

        assertEquals(List.of("UNKNOWN"), values);
    }

    private SelectField selectField(String id, String businessKey, String... optionValues) {
        SelectField field = new SelectField();
        field.setId(id);
        field.setBusinessKey(businessKey);
        field.setType("SELECT");
        List<OptionProp> options = new ArrayList<>();
        for (int i = 0; i < optionValues.length; i += 2) {
            options.add(new OptionProp(optionValues[i], optionValues[i + 1]));
        }
        field.setOptions(options);
        return field;
    }

    private FieldExportMeta meta(BaseField field, AbstractModuleFieldResolver<?> resolver) {
        FieldExportMeta meta = new FieldExportMeta();
        meta.setHead(field.getBusinessKey());
        meta.setField(field);
        meta.setFieldId(field.getId());
        meta.setBusinessKey(field.getBusinessKey());
        meta.setResolver(resolver);
        meta.setNoResource(true);
        return meta;
    }
}
