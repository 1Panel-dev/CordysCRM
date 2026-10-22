package cn.cordys.common.service;

import cn.cordys.common.context.SourceDetailResolveContext;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.form.domain.CustomFormDataField;
import cn.cordys.crm.form.service.CustomFormDataFieldService;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.InputField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldReferenceServiceTest {
    private final FieldSourceServiceProvider sources = mock(FieldSourceServiceProvider.class);
    private final FieldReferenceService service = new FieldReferenceService();

    FieldReferenceServiceTest() {
        ReflectionTestUtils.setField(service, "fieldSourceServiceProvider", sources);
    }

    @AfterEach
    void clearContexts() {
        while (SourceDetailResolveContext.getDepth() > 0) {
            SourceDetailResolveContext.end();
        }
        SourceDetailResolveContext.clear();
        CustomFormDataFieldService.clearFormKey();
    }

    @Test
    void businessReferencesUseOneBatchAndPreserveBusinessAndCustomValues() {
        DatasourceField source = source("CUSTOMER");
        source.setBusinessKey("customerId");
        source.setShowFields(List.of("name", "score"));
        InputField name = reference("name");
        name.setBusinessKey("name");
        InputField score = reference("score");
        when(sources.batchGetSimpleByIds(FieldSourceType.CUSTOMER, List.of("customer-1")))
                .thenReturn(List.of(Map.of("id", "customer-1", "name", "客户名称",
                        "moduleFields", List.of(Map.of("fieldId", "score", "fieldValue", 42)))));
        Map<String, List<BaseModuleFieldValue>> result = new HashMap<>();

        assertSame(result, service.setBusinessRefFieldValue(
                List.of(Map.of("id", "order-1", "customerId", "customer-1"),
                        Map.of("id", "order-2", "customerId", "customer-1")),
                List.of(source, name, score), result));

        for (String id : List.of("order-1", "order-2")) {
            assertEquals(List.of("source_ref_name", "source_ref_score"),
                    result.get(id).stream().map(BaseModuleFieldValue::getFieldId).toList());
            assertEquals(List.of("客户名称", 42),
                    result.get(id).stream().map(BaseModuleFieldValue::getFieldValue).toList());
        }
        verify(sources).batchGetSimpleByIds(FieldSourceType.CUSTOMER, List.of("customer-1"));
        assertEquals(0, SourceDetailResolveContext.getDepth());
        assertTrue(SourceDetailResolveContext.getSourceMap().isEmpty());
    }

    @Test
    void failedCustomReferenceRestoresParentFormAndClearsResolutionContext() {
        DatasourceField source = source("child-form");
        source.setBusinessKey("childId");
        CustomFormDataFieldService.setFormKey("parent-form");
        when(sources.batchGetSimpleByIds(FieldSourceType.CUSTOM_FORM, List.of("child-1")))
                .thenAnswer(invocation -> {
                    assertEquals("child-form", currentForm());
                    SourceDetailResolveContext.put("child-1", Map.of("id", "child-1"));
                    throw new IllegalStateException("详情转换失败");
                });

        assertThrows(IllegalStateException.class, () -> service.setBusinessRefFieldValue(
                List.of(Map.of("id", "parent-1", "childId", "child-1")),
                List.of(source, reference("name")), new HashMap<>()));

        assertEquals("parent-form", currentForm());
        assertEquals(0, SourceDetailResolveContext.getDepth());
        assertTrue(SourceDetailResolveContext.getSourceMap().isEmpty());
    }

    @Test
    void nestedCustomReadsKeepPlaceholdersAndRestoreOuterContext() {
        DatasourceField source = source("child-form");
        CustomFormDataField field = new CustomFormDataField();
        field.setFieldId("source");
        field.setFieldValue("child-1");
        CustomFormDataFieldService.setFormKey("parent-form");
        when(sources.batchGetSimpleByIds(FieldSourceType.CUSTOM_FORM, List.of("child-1")))
                .thenAnswer(invocation -> {
                    assertEquals("child-form", currentForm());
                    assertTrue(SourceDetailResolveContext.contains("child-1"));
                    SourceDetailResolveContext.start();
                    try {
                        service.loadSourceDetails(List.of(source), List.of(field));
                    } finally {
                        SourceDetailResolveContext.end();
                    }
                    return List.of(Map.of("id", "child-1", "name", "子记录"));
                });

        SourceDetailResolveContext.start();
        try {
            service.loadSourceDetails(List.of(source), List.of(field, field));
            service.loadSourceDetails(List.of(source), List.of(field));
            assertEquals("子记录", SourceDetailResolveContext.getSourceMap().get("child-1").get("name"));
            assertEquals("parent-form", currentForm());
            assertEquals(1, SourceDetailResolveContext.getDepth());
        } finally {
            SourceDetailResolveContext.end();
        }
        verify(sources).batchGetSimpleByIds(FieldSourceType.CUSTOM_FORM, List.of("child-1"));
    }

    @Test
    void customFormScopeCleansUpWhenThereWasNoParent() {
        assertEquals("child-form", CustomFormDataFieldService.withFormKey("child-form", this::currentForm));
        assertThrows(IllegalStateException.class, this::currentForm);
        assertThrows(IllegalArgumentException.class,
                () -> CustomFormDataFieldService.withFormKey("child-form", () -> {
                    throw new IllegalArgumentException("失败");
                }));
        assertThrows(IllegalStateException.class, this::currentForm);
    }

    @Test
    void titleAndSubtableReferencesRetainTheirSpecialKeys() {
        DatasourceField source = source("BUSINESS_TITLE");
        InputField titleName = reference("business_title_name");
        assertEquals("抬头", service.getFieldValueOfDetailMap(titleName, Map.of("name", "抬头"), source));
        Map<String, Object> detail = Map.of("products",
                List.of(Map.of("id", "row-1", "price", 12), Map.of("id", "row-2", "price", 20)));
        assertEquals(20, service.matchSubFieldValueOfDetailMap("price", detail, "products", "row-2"));
        assertEquals(12, service.matchSubFieldValueOfDetailMap("source_ref_price", detail, "products", "row-1"));
        assertNull(service.matchSubFieldValueOfDetailMap("price", detail, "products", "missing"));
    }

    private DatasourceField source(String type) {
        DatasourceField field = new DatasourceField();
        field.setId("source");
        field.setType("DATA_SOURCE");
        field.setDataSourceType(type);
        field.setShowFields(List.of("name"));
        return field;
    }

    private InputField reference(String key) {
        InputField field = new InputField();
        field.setId("source_ref_" + key);
        field.setResourceFieldId("source");
        return field;
    }

    private String currentForm() {
        return ReflectionTestUtils.invokeMethod(new CustomFormDataFieldService(), "getFormKey");
    }
}
