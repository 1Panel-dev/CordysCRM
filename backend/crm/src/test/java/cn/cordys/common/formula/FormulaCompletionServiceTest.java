package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.FormulaField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.InputNumberField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaCompletionServiceTest {

    private final FormulaCompletionService service = new FormulaCompletionService(
            new FormulaEngine(),
            new FormulaResultNormalizer(),
            Clock.fixed(Instant.parse("2026-09-02T09:09:11Z"), ZoneId.of("Asia/Shanghai")),
            (field, rawValue) -> "customer".equals(field.getId()) ? "Cordys" : rawValue
    );

    @Test
    void springSelectsProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(FormulaEngine.class, FormulaCompletionService.class);
            context.refresh();

            assertNotNull(context.getBean(FormulaCompletionService.class));
        }
    }

    @Test
    void completesQuotationNameAndPreservesSerialNumberPlaceholderContract() {
        DatasourceField customer = field(new DatasourceField(), "customer", "客户工商抬头", "DATA_SOURCE");
        SerialNumberField serialNumber = field(new SerialNumberField(), "quotationNo", "报价编号", "SERIAL_NUMBER");
        InputField quotationName = field(new InputField(), "quotationName", "报价单名称", "INPUT");
        quotationName.setDefaultValueType("formula");
        quotationName.setFormula(formula(fn("CONCATENATE",
                fieldNode("customer"), literal("-", "string"), fieldNode("quotationNo"))));
        Map<String, BaseModuleFieldValue> values = new LinkedHashMap<>();
        values.put("customer", new BaseModuleFieldValue("customer", "customer-id"));
        values.put("quotationName", new BaseModuleFieldValue("quotationName", "模型伪造的值"));

        service.complete(List.of(customer, serialNumber, quotationName), values,
                true, businessKey -> null, (businessKey, value) -> { });

        assertEquals("Cordys-${报价编号}", values.get("quotationName").getFieldValue());
    }

    @Test
    void updateFormulaUsesPersistedSerialNumberValue() {
        SerialNumberField serialNumber = field(
                new SerialNumberField(), "quotationNo", "报价编号", "SERIAL_NUMBER");
        InputField quotationName = field(new InputField(), "quotationName", "报价单名称", "INPUT");
        quotationName.setDefaultValueType("formula");
        quotationName.setFormula(formula(fn("CONCATENATE",
                literal("Q-", "string"), fieldNode("quotationNo"))));
        Map<String, BaseModuleFieldValue> values = new LinkedHashMap<>();
        values.put("quotationNo", new BaseModuleFieldValue("quotationNo", "BJ0001"));

        service.complete(List.of(serialNumber, quotationName), values,
                false, businessKey -> null, (businessKey, value) -> { });

        assertEquals("Q-BJ0001", values.get("quotationName").getFieldValue());
    }

    @Test
    void updateFormulaDoesNotReuseSubmittedFormulaResult() {
        InputNumberField amount = field(new InputNumberField(), "amount", "金额", "INPUT_NUMBER");
        FormulaField doubled = numberFormula("doubled", "两倍金额",
                binary("*", fieldNode("amount"), literal(2, "number")));
        Map<String, BaseModuleFieldValue> values = new LinkedHashMap<>();
        values.put("amount", new BaseModuleFieldValue("amount", 6));
        values.put("doubled", new BaseModuleFieldValue("doubled", 999));

        service.complete(List.of(amount, doubled), values,
                false, businessKey -> null, (businessKey, value) -> { });

        assertEquals(12D, values.get("doubled").getFieldValue());
    }

    @Test
    void calculatesFormulaDependenciesInTopologicalOrderAndOverridesSubmittedValues() {
        InputNumberField amount = field(new InputNumberField(), "amount", "金额", "INPUT_NUMBER");
        FormulaField doubled = numberFormula("doubled", "两倍金额",
                binary("*", fieldNode("amount"), literal(2, "number")));
        FormulaField plusOne = numberFormula("plusOne", "加一金额",
                binary("+", fieldNode("doubled"), literal(1, "number")));
        Map<String, BaseModuleFieldValue> values = new LinkedHashMap<>();
        values.put("amount", new BaseModuleFieldValue("amount", 5));
        values.put("doubled", new BaseModuleFieldValue("doubled", 999));
        values.put("plusOne", new BaseModuleFieldValue("plusOne", 999));

        service.complete(List.of(plusOne, doubled, amount), values,
                true, businessKey -> null, (businessKey, value) -> { });

        assertEquals(10D, values.get("doubled").getFieldValue());
        assertEquals(11D, values.get("plusOne").getFieldValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void calculatesSubTableRowsBeforeMainTableAggregation() {
        InputNumberField price = field(new InputNumberField(), "priceId", "单价", "INPUT_NUMBER");
        price.setBusinessKey("price");
        InputNumberField quantity = field(new InputNumberField(), "quantityId", "数量", "INPUT_NUMBER");
        quantity.setBusinessKey("quantity");
        FormulaField rowAmount = numberFormula("rowAmountId", "行金额",
                binary("*", fieldNode("price"), fieldNode("quantity")));
        rowAmount.setBusinessKey("rowAmount");
        SubField items = field(new SubField(), "items", "产品明细", "SUB_PRODUCT");
        items.setSubFields(List.of(price, quantity, rowAmount));
        FormulaField total = numberFormula("total", "总金额", fn("SUM", fieldNode("items.rowAmount")));

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(new LinkedHashMap<>(Map.of("price", 2, "quantity", 3)));
        rows.add(new LinkedHashMap<>(Map.of("price", 4, "quantity", 5)));
        Map<String, BaseModuleFieldValue> values = new LinkedHashMap<>();
        values.put("items", new BaseModuleFieldValue("items", rows));

        service.complete(List.of(total, items), values,
                true, businessKey -> null, (businessKey, value) -> { });

        List<Map<String, Object>> calculatedRows =
                (List<Map<String, Object>>) values.get("items").getFieldValue();
        assertEquals(6D, calculatedRows.get(0).get("rowAmount"));
        assertEquals(20D, calculatedRows.get(1).get("rowAmount"));
        assertEquals(26D, values.get("total").getFieldValue());
    }

    @Test
    void rejectsCircularFormulaDependencies() {
        FormulaField first = numberFormula("first", "公式一",
                binary("+", fieldNode("second"), literal(1, "number")));
        FormulaField second = numberFormula("second", "公式二",
                binary("+", fieldNode("first"), literal(1, "number")));

        FormulaEvaluationException exception = assertThrows(FormulaEvaluationException.class,
                () -> service.complete(List.of(first, second), new LinkedHashMap<>(),
                        true, businessKey -> null, (businessKey, value) -> { }));

        assertTrue(exception.getMessage().contains("循环依赖"));
    }

    @Test
    void preservesTextResultEvenWhenFormulaDisplayModeIsNumericLikeFrontend() {
        FormulaField numeric = numberFormula("numeric", "数字结果",
                literal("12.349", "string"));
        Map<String, BaseModuleFieldValue> values = new LinkedHashMap<>();

        service.complete(List.of(numeric), values,
                true, businessKey -> null, (businessKey, value) -> { });

        assertEquals("12.349", values.get("numeric").getFieldValue());
    }

    private FormulaField numberFormula(String id, String name, Map<String, Object> ir) {
        FormulaField field = field(new FormulaField(), id, name, "FORMULA");
        field.setFormulaResultFormat("number");
        field.setDecimalPlaces(true);
        field.setPrecision(2);
        field.setFormula(formula(ir));
        return field;
    }

    private <T extends BaseField> T field(T field, String id, String name, String type) {
        field.setId(id);
        field.setName(name);
        field.setType(type);
        return field;
    }

    private String formula(Map<String, Object> ir) {
        return JSON.toJSONString(Map.of(
                "source", "test",
                "display", "test",
                "fields", List.of(),
                "ir", ir
        ));
    }

    private Map<String, Object> fieldNode(String fieldId) {
        return Map.of("type", "field", "fieldId", fieldId);
    }

    private Map<String, Object> literal(Object value, String valueType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "literal");
        result.put("value", value);
        result.put("valueType", valueType);
        return result;
    }

    @SafeVarargs
    private Map<String, Object> fn(String name, Map<String, Object>... args) {
        return Map.of("type", "function", "name", name, "args", List.of(args));
    }

    private Map<String, Object> binary(String operator, Map<String, Object> left, Map<String, Object> right) {
        return Map.of("type", "binary", "operator", operator, "left", left, "right", right);
    }
}
