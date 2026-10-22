package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SerialNumberFormulaServiceTest {

    private final SerialNumberFormulaService service = new SerialNumberFormulaService(new FormulaEngine());

    @Test
    void springInjectsFormulaEngine() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(FormulaEngine.class, SerialNumberFormulaService.class);
            context.refresh();

            assertNotNull(context.getBean(SerialNumberFormulaService.class));
        }
    }

    @Test
    void selectsOnlyLocalInputFormulasReferencingCurrentSerialNumber() {
        SerialNumberField serial = serialField();
        InputField local = formulaField("local", "number");
        local.setDefaultValueType("FORMULA");
        InputField reference = formulaField("reference", "number");
        reference.setResourceFieldId("customer");
        InputField anotherSerial = formulaField("another", "other-number");
        InputField customDefault = formulaField("custom", "number");
        customDefault.setDefaultValueType("custom");
        InputField blank = formulaField("blank", "number");
        blank.setFormula(" ");

        assertEquals(List.of(local), service.serialNumberFormulaFields(
                List.of(serial, local, reference, anotherSerial, customDefault, blank), serial));
    }

    @Test
    void replacesBusinessAndCustomValuesOnlyForMatchingResourceAndPreservesNulls() {
        Order order = new Order();
        order.setId("order-1");
        order.setName("订单-${订单编号}-${订单编号}");
        InputField name = formulaField("name", "number");
        name.setBusinessKey("name");
        InputField custom = formulaField("custom", "number");
        InputField empty = formulaField("empty", "number");
        InputField missing = formulaField("missing", "number");
        BaseResourceSubField anotherOrder = value("order-2", "custom", "${订单编号}");
        BaseResourceSubField ownValue = value("order-1", "custom", "自定义-${订单编号}");
        BaseResourceSubField emptyValue = value("order-1", "empty", null);
        BaseResourceSubField unrelated = value("order-1", "unrelated", "${订单编号}");
        List<BaseResourceSubField> values = List.of(anotherOrder, ownValue, emptyValue, unrelated);
        String serialNo = "NO$1\\0001";

        service.fillSerialNumberFormulas(order, serialField(), List.of(name, custom, empty, missing), values, serialNo);

        assertEquals("订单-" + serialNo + "-" + serialNo, order.getName());
        assertEquals("自定义-" + serialNo, ownValue.getFieldValue());
        assertEquals("${订单编号}", anotherOrder.getFieldValue());
        assertEquals("${订单编号}", unrelated.getFieldValue());
        assertNull(emptyValue.getFieldValue());

        order.setName(null);
        service.fillSerialNumberFormulas(order, serialField(), List.of(name), List.of(), serialNo);
        assertNull(order.getName());
    }

    private SerialNumberField serialField() {
        SerialNumberField field = new SerialNumberField();
        field.setId("number");
        field.setName("订单编号");
        return field;
    }

    private InputField formulaField(String id, String referencedFieldId) {
        InputField field = new InputField();
        field.setId(id);
        field.setDefaultValueType("formula");
        field.setFormula(JSON.toJSONString(Map.of(
                "ir", Map.of("type", "field", "fieldId", referencedFieldId))));
        return field;
    }

    private BaseResourceSubField value(String resourceId, String fieldId, String value) {
        BaseResourceSubField field = new BaseResourceSubField();
        field.setResourceId(resourceId);
        field.setFieldId(fieldId);
        field.setFieldValue(value);
        return field;
    }
}
