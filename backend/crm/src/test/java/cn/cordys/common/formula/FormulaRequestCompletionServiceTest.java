package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationAddRequest;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ModuleFormService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
class FormulaRequestCompletionServiceTest {

    @Test
    void fillsBusinessFormulaBeforeRequestValidationWithoutAdditionalRequestArguments() {
        SerialNumberField quotationNumber = field(
                new SerialNumberField(), "quotationNumber", "报价编号", "SERIAL_NUMBER");
        quotationNumber.setBusinessKey("no");
        InputField quotationName = field(new InputField(), "quotationName", "报价单名称", "INPUT");
        quotationName.setBusinessKey("name");
        quotationName.setDefaultValueType("formula");
        quotationName.setFormula(formula(function("CONCATENATE",
                literal("Q-", "string"), fieldNode("quotationNumber"))));
        ModuleFormService moduleFormService = new ModuleFormService() {
            @Override
            public List<BaseField> getAllFields(String formKey, String organizationId) {
                assertEquals("quotation", formKey);
                return List.of(quotationNumber, quotationName);
            }
        };
        FormulaCompletionService completionService = new FormulaCompletionService(new FormulaEngine());
        FormulaRequestCompletionService service = new FormulaRequestCompletionService(
                moduleFormService, completionService);

        OpportunityQuotationAddRequest request = new OpportunityQuotationAddRequest();
        request.setModuleFields(new ArrayList<>());
        service.complete("quotation", request);

        assertEquals("Q-${报价编号}", request.getName());
        assertNotNull(request.getModuleFields());
    }

    @Test
    void preservesFormulaValueAlreadyCalculatedByFrontend() {
        SerialNumberField quotationNumber = field(
                new SerialNumberField(), "quotationNumber", "报价编号", "SERIAL_NUMBER");
        quotationNumber.setBusinessKey("number");
        InputField quotationName = field(new InputField(), "quotationName", "报价单名称", "INPUT");
        quotationName.setBusinessKey("name");
        quotationName.setDefaultValueType("formula");
        quotationName.setFormula(formula(function("CONCATENATE",
                literal("Q-", "string"), fieldNode("quotationNumber"))));
        ModuleFormService moduleFormService = new ModuleFormService() {
            @Override
            public List<BaseField> getAllFields(String formKey, String organizationId) {
                return List.of(quotationNumber, quotationName);
            }
        };
        FormulaRequestCompletionService service = new FormulaRequestCompletionService(
                moduleFormService, new FormulaCompletionService(new FormulaEngine()));
        FormulaUpdateRequest request = new FormulaUpdateRequest();
        request.setNumber("BJ0001");
        request.setName("前端已计算的公式值");
        request.setModuleFields(new ArrayList<>());

        service.complete("quotation", request, false);

        assertEquals("前端已计算的公式值", request.getName());
    }

    @Test
    void calculatesMissingUpdateFormulaWithExistingSerialNumber() {
        SerialNumberField quotationNumber = field(
                new SerialNumberField(), "quotationNumber", "报价编号", "SERIAL_NUMBER");
        quotationNumber.setBusinessKey("number");
        InputField quotationName = field(new InputField(), "quotationName", "报价单名称", "INPUT");
        quotationName.setBusinessKey("name");
        quotationName.setDefaultValueType("formula");
        quotationName.setFormula(formula(function("CONCATENATE",
                literal("Q-", "string"), fieldNode("quotationNumber"))));
        ModuleFormService moduleFormService = new ModuleFormService() {
            @Override
            public List<BaseField> getAllFields(String formKey, String organizationId) {
                return List.of(quotationNumber, quotationName);
            }
        };
        FormulaRequestCompletionService service = new FormulaRequestCompletionService(
                moduleFormService, new FormulaCompletionService(new FormulaEngine()));
        FormulaUpdateRequest request = new FormulaUpdateRequest();
        request.setNumber("BJ0001");
        request.setModuleFields(new ArrayList<>());

        service.complete("quotation", request, false);

        assertEquals("Q-BJ0001", request.getName());
    }

    @Test
    void updateDoesNotInventSerialNumberWhenCurrentValueIsMissing() {
        SerialNumberField quotationNumber = field(
                new SerialNumberField(), "quotationNumber", "报价编号", "SERIAL_NUMBER");
        quotationNumber.setBusinessKey("number");
        InputField quotationName = field(new InputField(), "quotationName", "报价单名称", "INPUT");
        quotationName.setBusinessKey("name");
        quotationName.setDefaultValueType("formula");
        quotationName.setFormula(formula(function("CONCATENATE",
                literal("Q-", "string"), fieldNode("quotationNumber"))));
        ModuleFormService moduleFormService = new ModuleFormService() {
            @Override
            public List<BaseField> getAllFields(String formKey, String organizationId) {
                return List.of(quotationNumber, quotationName);
            }
        };
        FormulaRequestCompletionService service = new FormulaRequestCompletionService(
                moduleFormService, new FormulaCompletionService(new FormulaEngine()));
        FormulaUpdateRequest request = new FormulaUpdateRequest();
        request.setModuleFields(new ArrayList<>());

        service.complete("quotation", request, false);

        assertEquals("Q-", request.getName());
    }

    public static class FormulaUpdateRequest {

        private String name;
        private String number;
        private List<BaseModuleFieldValue> moduleFields;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public List<BaseModuleFieldValue> getModuleFields() {
            return moduleFields;
        }

        public void setModuleFields(List<BaseModuleFieldValue> moduleFields) {
            this.moduleFields = moduleFields;
        }
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
    private Map<String, Object> function(String name, Map<String, Object>... args) {
        return Map.of("type", "function", "name", name, "args", List.of(args));
    }
}
