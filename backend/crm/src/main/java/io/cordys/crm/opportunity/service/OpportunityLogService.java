package io.cordys.crm.opportunity.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityLogService extends BaseModuleLogService {


    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.OPPORTUNITY.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey())) {
                setProductName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey())) {
                setContactFieldName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_CUSTOMER_NAME.getBusinessKey())) {
                differ.setColumnName(Translator.get("log.customerId"));
                setCustomerName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumnName(), Translator.get("log.stage"))) {
                differ.setOldValueName(Translator.get(differ.getOldValue().toString()));
                differ.setNewValueName(Translator.get(differ.getNewValue().toString()));
            }

            if (StringUtils.equals(differ.getColumn(), "status")) {
                differ.setColumnName(Translator.get("log.opportunity." + differ.getColumn()));
                differ.setOldValueName(Boolean.parseBoolean(differ.getOldValueName().toString()) ? Translator.get("log.opportunity.status.true") : Translator.get("log.opportunity.status.false"));
                differ.setNewValueName(Boolean.parseBoolean(differ.getNewValueName().toString()) ? Translator.get("log.opportunity.status.true") : Translator.get("log.opportunity.status.false"));
            }

            if (StringUtils.equals(differ.getColumn(), "expectedEndTime")) {
                setFormatDataTimeFieldValueName(differ);
            }
        }
    }


}
