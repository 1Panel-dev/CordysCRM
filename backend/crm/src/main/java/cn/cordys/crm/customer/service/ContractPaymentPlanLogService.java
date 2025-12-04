package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractPaymentPlanLogService extends BaseModuleLogService {

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        return super.handleModuleLogField(differences, orgId, FormKey.CONTRACT_PAYMENT_PLAN.getKey());
    }
}