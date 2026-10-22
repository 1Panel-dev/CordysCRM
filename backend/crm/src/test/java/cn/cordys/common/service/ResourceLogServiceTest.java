package cn.cordys.common.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.field.AttachmentField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtAttachmentMapper;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceLogServiceTest {

    private final ResourceLogService logs = new ResourceLogService();
    private final BaseService service = new BaseService();
    private final ExtAttachmentMapper attachments = mock(ExtAttachmentMapper.class);
    private final ExtModuleFieldMapper fields = mock(ExtModuleFieldMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ModuleField> moduleFields = mock(BaseMapper.class);
    private final Customer customer = new Customer();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(logs, "extAttachmentMapper", attachments);
        ReflectionTestUtils.setField(logs, "extModuleFieldMapper", fields);
        ReflectionTestUtils.setField(logs, "moduleFieldMapper", moduleFields);
        ReflectionTestUtils.setField(service, "resourceLogService", logs);
        customer.setId("customer-1");
        customer.setName("客户名称");
        OperationLogContext.putEmptySpan();
    }

    @AfterEach
    void clearContext() {
        OperationLogContext.clear();
    }

    @Test
    void addLogPreservesResourceIdentityAndFiltersOnlyInvalidValues() {
        service.handleAddLogWithResourceName(customer, List.of(
                new BaseModuleFieldValue("text", "value"),
                new BaseModuleFieldValue("blank", " "),
                new BaseModuleFieldValue("empty", List.of()),
                new BaseModuleFieldValue("null", null),
                new BaseModuleFieldValue("zero", 0),
                new BaseModuleFieldValue("false", false)));

        var context = OperationLogContext.getContext();
        Map<?, ?> values = (Map<?, ?>) context.getModifiedValue();
        assertEquals("customer-1", context.getResourceId());
        assertEquals("客户名称", context.getResourceName());
        assertEquals("value", values.get("text"));
        assertEquals(0, values.get("zero"));
        assertEquals(false, values.get("false"));
        assertFalse(values.containsKey("blank"));
        assertFalse(values.containsKey("empty"));
        assertFalse(values.containsKey("null"));
        assertNull(context.getOriginalValue());
        verifyNoInteractions(attachments, fields, moduleFields);
    }

    @Test
    void updateLogRetainsAttachmentNamesForBothJsonAndListInputs() {
        ModuleField attachment = new ModuleField();
        attachment.setId("attachment");
        when(moduleFields.selectListByLambda(any())).thenReturn(List.of(attachment));
        when(attachments.selectNameByIds(List.of("old-id"))).thenReturn(List.of("旧附件.pdf"));
        when(attachments.selectNameByIds(List.of("new-id"))).thenReturn(List.of("新附件.pdf"));

        service.handleUpdateLog(customer, customer,
                List.of(new BaseModuleFieldValue("attachment", "[\"old-id\"]")),
                List.of(new BaseModuleFieldValue("attachment", List.of("new-id"))),
                "customer-1", "客户名称");

        var context = OperationLogContext.getContext();
        assertEquals(List.of("旧附件.pdf"), ((Map<?, ?>) context.getOriginalValue()).get("attachment"));
        assertEquals(List.of("新附件.pdf"), ((Map<?, ?>) context.getModifiedValue()).get("attachment"));
        assertEquals("customer-1", context.getResourceId());
        assertEquals("客户名称", context.getResourceName());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void subTableLogsPreserveBusinessKeysRowLabelsAndReferenceExclusion(int rowCount) {
        InputField quantity = new InputField();
        quantity.setId("quantity-id");
        quantity.setBusinessKey("quantity");
        quantity.setName("数量");
        InputField reference = new InputField();
        reference.setId("reference");
        reference.setResourceFieldId("source-field");
        reference.setName("引用字段");
        SubField products = new SubField();
        products.setId("products-id");
        products.setBusinessKey("products");
        products.setType(FieldType.SUB_PRODUCT.name());
        products.setName("产品明细");
        products.setSubFields(List.of(quantity, reference));
        AttachmentField attachment = new AttachmentField();
        attachment.setId("attachment");
        attachment.setType(FieldType.ATTACHMENT.name());
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(List.of(products, attachment));
        when(fields.getSourceOptionsByIds(eq("sys_module_field"), anyList())).thenReturn(List.of());
        when(attachments.selectNameByIds(List.of("file-id"))).thenReturn(List.of("附件.pdf"));
        List<Map<String, Object>> rows = IntStream.rangeClosed(1, rowCount)
                .mapToObj(index -> Map.<String, Object>of("quantity", index, "reference", "ignored", "rowId", "id"))
                .toList();
        List<BaseModuleFieldValue> values = List.of(new BaseModuleFieldValue("products", rows),
                new BaseModuleFieldValue("attachment", List.of("file-id")));

        try (MockedStatic<Translator> translator = mockStatic(Translator.class)) {
            translator.when(() -> Translator.get("row")).thenReturn("行");
            service.handleUpdateLogWithSubTable(customer, customer, values, values,
                    "customer-1", "客户名称", "默认子表", config);
        }

        Map<?, ?> before = (Map<?, ?>) OperationLogContext.getContext().getOriginalValue();
        Map<?, ?> after = (Map<?, ?>) OperationLogContext.getContext().getModifiedValue();
        assertEquals(before, after);
        for (int row = 1; row <= rowCount; row++) {
            String key = "产品明细-数量-" + (rowCount > 1 ? "行" + row + "-" : "") + "quantity";
            assertEquals(row, after.get(key));
        }
        assertFalse(after.keySet().stream().anyMatch(key -> key.toString().contains("reference")));
        assertFalse(after.containsKey("products"));
        assertEquals(List.of("附件.pdf"), after.get("attachment"));
        assertEquals(Map.of("products-id", "products"), service.getSubTableIdKeyMap(config));
    }
}
