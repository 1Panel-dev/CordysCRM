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
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences = super.handleModuleLogField(differences, orgId, FormKey.CLUE.getKey());

        for (JsonDifferenceDTO difference : differences) {
            if (Strings.CS.equals(difference.getColumn(), BusinessModuleField.CLUE_OWNER.getBusinessKey())) {
                setUserFieldName(difference);
                continue;
            }

            if (Strings.CS.equals(difference.getColumn(), BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey())) {
                setProductName(difference);
            }

            if (Strings.CS.equals(difference.getColumn(), "stage")) {
                difference.setColumnName(Translator.get("clue.stage"));
                difference.setNewValueName(ClueStatus.getByKey((String) difference.getNewValue()));
                difference.setOldValueName(ClueStatus.getByKey((String) difference.getOldValue()));
            }
        }

        return differences;
    }
}
