package io.cordys.crm.follow.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpRecordLogService extends BaseModuleLogService {

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.FOLLOW_RECORD.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_CUSTOMER.getBusinessKey())) {
                setCustomerName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_CLUE.getBusinessKey())) {
                setClueName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_CONTENT.getBusinessKey())) {
                differ.setColumnName(Translator.get("log.follow_record_content"));
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_CONTACT.getBusinessKey())) {
                setContactFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_TIME.getBusinessKey())) {
                setFormatDataTimeFieldValueName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_OPPORTUNITY.getBusinessKey())) {
                setOpportunityName(differ);
                continue;
            }
        }
    }

}
