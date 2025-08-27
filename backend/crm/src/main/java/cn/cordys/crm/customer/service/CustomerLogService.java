package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.Strings;
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
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.CUSTOMER_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
            } else if (Strings.CS.equals(differ.getColumn(), "collectionTime")) {
                setFormatDataTimeFieldValueName(differ);
            }
        }
    }
}