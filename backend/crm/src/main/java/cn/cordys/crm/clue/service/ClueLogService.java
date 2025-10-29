package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ClueLogService extends BaseModuleLogService {


    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        differenceDTOS = super.handleModuleLogField(differenceDTOS, orgId, FormKey.CLUE.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.CLUE_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey())) {
                setProductName(differ);
            }

            if (Strings.CS.equals(differ.getColumn(), "stage")) {
                differ.setColumnName(Translator.get("clue.stage"));
                differ.setNewValueName(ClueStatus.getByKey((String) differ.getNewValue()));
                differ.setOldValueName(ClueStatus.getByKey((String) differ.getOldValue()));
            }
        }

        return differenceDTOS;
    }
}
