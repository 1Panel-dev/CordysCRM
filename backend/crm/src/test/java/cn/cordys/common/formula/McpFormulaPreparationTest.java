package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.form.service.CustomFormService;
import cn.cordys.crm.system.controller.McpFormulaController;
import cn.cordys.crm.system.dto.field.FormulaField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.service.ModuleFormService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class McpFormulaPreparationTest {
    @Test
    void MCP新增和更新预计算名称且不修改输入或生成新编号() {
        InputField input = field(new InputField(), "input", "INPUT");
        SerialNumberField serial = field(new SerialNumberField(), "serial", "SERIAL_NUMBER");
        serial.setBusinessKey("no");
        InputField name = field(new InputField(), "name", "INPUT");
        name.setBusinessKey("name");
        name.setDefaultValueType("formula");
        name.setFormula(formula(Map.of("type", "function", "name", "CONCATENATE", "args", List.of(
                Map.of("type", "field", "fieldId", "serial"), Map.of("type", "field", "fieldId", "input")))));
        FormulaRequestCompletionService service = service(List.of(input, serial, name));
        Map<String, Object> create = Map.of("moduleFields", List.of(new BaseModuleFieldValue("input", "新值")));
        assertEquals("${serial}新值", service.prepare("quotation", create).get("name"));
        assertFalse(create.containsKey("name"));
        Map<String, Object> update = Map.of("id", "record-1", "no", "Q001", "name", "旧结果",
                "moduleFields", List.of(new BaseModuleFieldValue("input", "新值")));
        Map<String, Object> result = service.prepare("quotation", update);
        assertEquals("Q001新值", result.get("name"));
        assertEquals("Q001", result.get("no"));
        assertEquals("旧结果", update.get("name"));
    }

    @Test
    void MCP子表结果截断且保留行身份和其它字段() {
        FormulaField total = field(new FormulaField(), "total", "FORMULA");
        total.setFormulaResultFormat("number");
        total.setDecimalPlaces(true);
        total.setPrecision(2);
        total.setFormula(formula(Map.of("type", "literal", "valueType", "number", "value", -12.349)));
        SubField table = field(new SubField(), "table", "SUB_PRODUCT");
        table.setSubFields(List.of(total));
        Map<String, Object> original = Map.of("moduleFields", List.of(new BaseModuleFieldValue("table",
                List.of(Map.of("id", "row-1", "total", 999, "extra", "保留")))));
        String before = JSON.toJSONString(original);
        Map<String, Object> result = service(List.of(table)).prepare("quotation", original);
        BaseModuleFieldValue field = (BaseModuleFieldValue) ((List<?>) result.get("moduleFields")).getFirst();
        Map<?, ?> row = (Map<?, ?>) ((List<?>) field.getFieldValue()).getFirst();
        assertEquals("-12.34", row.get("total").toString());
        assertEquals("row-1", row.get("id"));
        assertEquals("保留", row.get("extra"));
        assertEquals(before, JSON.toJSONString(original));
    }

    @Test
    void CRM普通请求不存在公式全局拦截器() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(RequestBodyAdvice.class));
        assertTrue(scanner.findCandidateComponents("cn.cordys.common.formula").isEmpty());
    }

    @Test
    void 专用入口拒绝未授权表单且不开始计算() {
        FormulaRequestCompletionService completion = mock(FormulaRequestCompletionService.class);
        McpFormulaController controller = new McpFormulaController(completion, mock(CustomFormService.class));
        try (var permission = mockStatic(PermissionUtils.class)) {
            permission.when(() -> PermissionUtils.hasPermission(anyString())).thenReturn(false);
            assertThrows(GenericException.class, () -> controller.prepare(
                    new McpFormulaController.PrepareRequest("quotation", Map.of())));
            verifyNoInteractions(completion);
        }
    }

    private FormulaRequestCompletionService service(List<BaseField> fields) {
        ModuleFormService forms = mock(ModuleFormService.class);
        when(forms.getAllFields(anyString(), nullable(String.class))).thenReturn(fields);
        return new FormulaRequestCompletionService(forms, new FormulaCompletionService(new FormulaEngine()));
    }

    private <T extends BaseField> T field(T field, String id, String type) {
        field.setId(id);
        field.setName(id);
        field.setType(type);
        return field;
    }

    private String formula(Map<String, Object> ir) {
        return JSON.toJSONString(Map.of("ir", ir));
    }
}
