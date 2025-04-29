package io.cordys.crm.follow.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpPlanLogService extends BaseModuleLogService {

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.FOLLOW_PLAN.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_CUSTOMER.getBusinessKey())) {
                setCustomerName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_CONTENT.getBusinessKey())) {
                differ.setColumnName(Translator.get("log.follow_record_content"));
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_CONTACT.getBusinessKey())) {
                setContactFieldName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_ESTIMATED_TIME.getBusinessKey())) {
                differ.setOldValueName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Long.parseLong(differ.getOldValue().toString())));
                differ.setNewValueName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Long.parseLong(differ.getNewValue().toString())));
                continue;
            }


        }
    }

}
