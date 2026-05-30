package cn.cordys.crm.product.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class PriceLogService extends BaseModuleLogService {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences = super.handleModuleLogField(differences, orgId, FormKey.PRICE.getKey());

        for (JsonDifferenceDTO differ : differences) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.PRICE_STATUS.getBusinessKey())) {
                setPriceStatus(differ, orgId);
            }
            if (differ.getColumn().contains("-")) {
                differ.setColumnName(differ.getColumn());
            }
        }

        return differences;
    }


    /**
     * 产品
     *
     * @param differ businessModuleField
     */
    private void setPriceStatus(JsonDifferenceDTO differ, String orgId) {
        ModuleFormConfigDTO priceConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), orgId);
        Optional<BaseField> statusField = priceConfig.getFields().stream().filter(f -> Strings.CS.equals(BusinessModuleField.PRICE_STATUS.getKey(), f.getInternalKey())).findFirst();
        if (statusField.isEmpty()) {
            return;
        }
        List<OptionProp> options = ((HasOption) statusField.get()).getOptions();
        differ.setNewValueName(options.stream()
                .filter(optionProp -> Strings.CS.equals(optionProp.getValue(), differ.getNewValue().toString())).map(OptionProp::getLabel));
        differ.setOldValueName(options.stream()
                .filter(optionProp -> Strings.CS.equals(optionProp.getValue(), differ.getOldValue().toString())).map(OptionProp::getLabel));
    }
}
