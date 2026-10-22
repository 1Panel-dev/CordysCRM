package cn.cordys.crm.contract.service;

import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractSerialNumberFormulaTest {

    @Test
    void preservesContractPrefixReplacementAfterRemovingExportInheritance() {
        Contract contract = new Contract();
        contract.setId("contract-1");
        contract.setName("采购-HT${合同编号}");
        InputField name = new InputField();
        name.setBusinessKey("name");
        InputField custom = new InputField();
        custom.setId("custom");
        SerialNumberField serial = new SerialNumberField();
        serial.setName("合同编号");
        BaseResourceSubField ownValue = value("contract-1");
        BaseResourceSubField otherValue = value("contract-2");
        List<BaseField> formulas = List.of(name, custom);

        ReflectionTestUtils.invokeMethod(new ContractService(), "fillSerialNumberFormulas", contract, serial,
                formulas, List.of(otherValue, ownValue), "HT0001");

        assertEquals("采购-HT0001", contract.getName());
        assertEquals("HT0001/HT0001", ownValue.getFieldValue());
        assertEquals("HT${合同编号}/${合同编号}", otherValue.getFieldValue());
    }

    private BaseResourceSubField value(String resourceId) {
        BaseResourceSubField field = new BaseResourceSubField();
        field.setResourceId(resourceId);
        field.setFieldId("custom");
        field.setFieldValue("HT${合同编号}/${合同编号}");
        return field;
    }
}
