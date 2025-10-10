package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.Strings;
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
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey())) {
                setProductName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey())) {
                setContactFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_CUSTOMER_NAME.getBusinessKey())) {
                differ.setColumnName(Translator.get("log.customerId"));
                setCustomerName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumnName(), Translator.get("log.stage"))) {
                if (differ.getOldValue() != null) {
                    differ.setOldValueName(Translator.get(differ.getOldValueName().toString(), differ.getOldValueName().toString()));
                }
                if (differ.getNewValue() != null) {
                    differ.setNewValueName(Translator.get(differ.getNewValueName().toString(), differ.getNewValueName().toString()));
                }
            }

            if (Strings.CS.equals(differ.getColumn(), "status")) {
                differ.setColumnName(Translator.get("log.opportunity." + differ.getColumn()));
                if (differ.getOldValue() != null) {
                    differ.setOldValueName(Boolean.parseBoolean(differ.getOldValueName().toString()) ? Translator.get("log.opportunity.status.true") : Translator.get("log.opportunity.status.false"));
                }
                if (differ.getNewValue() != null) {
                    differ.setNewValueName(Boolean.parseBoolean(differ.getNewValueName().toString()) ? Translator.get("log.opportunity.status.true") : Translator.get("log.opportunity.status.false"));
                }
            }

            if (Strings.CS.equals(differ.getColumn(), "expectedEndTime")) {
                setFormatDataTimeFieldValueName(differ);
            }

            if (Strings.CS.equals(differ.getColumn(), "actualEndTime")) {
                setFormatDataTimeFieldValueName(differ);
            }

            if (Strings.CS.equals(differ.getColumn(), "failureReason")) {
                if (differ.getOldValue() != null) {
                    differ.setOldValueName(Translator.get(differ.getOldValue().toString()));
                }
                if (differ.getNewValue() != null) {
                    differ.setNewValueName(Translator.get(differ.getNewValue().toString()));
                }
            }
        }
    }


}
