package io.cordys.crm.customer.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerLogService extends BaseModuleLogService {

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.CUSTOMER.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.CUSTOMER_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
            } else if (StringUtils.equals(differ.getColumn(), "collectionTime")) {
                setFormatDataTimeFieldValueName(differ);
            }
        }
    }
}