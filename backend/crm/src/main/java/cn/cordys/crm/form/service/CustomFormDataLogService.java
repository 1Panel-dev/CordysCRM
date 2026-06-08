package cn.cordys.crm.form.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormDataLogService extends BaseModuleLogService {

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        CustomFormData customFormData;
        if (oldValue != null) {
            customFormData = JSON.parseObject(oldValue, CustomFormData.class);
        } else {
            customFormData = JSON.parseObject(newValue, CustomFormData.class);
        }
        differences.removeIf(differ -> Strings.CS.equalsAny(differ.getColumn(), "customFormId"));
        differences = super.handleModuleLogField(differences, orgId, customFormData.getCustomFormId());
        for (JsonDifferenceDTO differ : differences) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.CUSTOM_FORM_DATA_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
            }
        }
        return differences;
    }
}
