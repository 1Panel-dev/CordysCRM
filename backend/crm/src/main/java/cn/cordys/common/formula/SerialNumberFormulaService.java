package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 导入生成流水号后，补齐引用该流水号的公式占位符。
 */
@Service
public class SerialNumberFormulaService {

    private final FormulaEngine formulaEngine;

    public SerialNumberFormulaService(FormulaEngine formulaEngine) {
        this.formulaEngine = formulaEngine;
    }

    /**
     * 取当前表单中「公式里引用了流水号字段」的计算字段。
     */
    public List<BaseField> serialNumberFormulaFields(List<BaseField> fields, BaseField serialField) {
        return fields.stream()
                .filter(field -> StringUtils.isBlank(field.getResourceFieldId()))
                .filter(field -> field instanceof InputField inputField
                        && StringUtils.isNotBlank(inputField.getFormula())
                        && Strings.CI.equals(inputField.getDefaultValueType(), "formula")
                        && formulaEngine.referencedFieldIds(inputField.getFormula()).contains(serialField.getId()))
                .toList();
    }

    /**
     * 公式中的占位符替换成真实流水号。
     *
     * @param resource       业务实体(Order/ContractPaymentRecord 等)
     * @param serialField    当前表单自己的流水号字段
     * @param formulaFields  引用了该流水号字段的公式类型字段
     * @param resourceFields 该实体的字段值行
     * @param serialNo       已生成的流水号
     */
    public <K> void fillSerialNumberFormulas(K resource, BaseField serialField, List<BaseField> formulaFields,
            List<BaseResourceSubField> resourceFields, String serialNo) {
        String placeholder = "${" + serialField.getName() + "}";
        BeanWrapper resourceWrapper = new BeanWrapperImpl(resource);
        Object resourceId = resourceWrapper.getPropertyValue("id");
        for (BaseField field : formulaFields) {
            if (field.hasBusinessKey()) {
                Object value = resourceWrapper.getPropertyValue(field.getBusinessKey());
                if (value != null) {
                    resourceWrapper.setPropertyValue(field.getBusinessKey(),
                            value.toString().replace(placeholder, serialNo));
                }
                continue;
            }
            BaseResourceSubField resourceField = resourceFields.stream()
                    .filter(item -> Objects.equals(resourceId, item.getResourceId())
                            && StringUtils.equals(field.getId(), item.getFieldId()))
                    .findFirst()
                    .orElse(null);
            if (resourceField != null && resourceField.getFieldValue() != null) {
                resourceField.setFieldValue(resourceField.getFieldValue().toString().replace(placeholder, serialNo));
            }
        }
    }
}
