package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractPaymentPlanLogService extends BaseModuleLogService {

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences = super.handleModuleLogField(differences, orgId, FormKey.CONTRACT_PAYMENT_PLAN.getKey());

        for (JsonDifferenceDTO differ : differences) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.CONTRACT_PAYMENT_PLAN_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
            } else if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.CONTRACT_PAYMENT_PLAN_CONTRACT.getBusinessKey())) {
                setContractFieldName(differ);
            } else if (Strings.CS.equals(differ.getColumn(), "planStatus")) {
                setPlanStatusFieldName(differ);
            }
        }
        return differences;
    }

    /**
     * 回款计划状态
     *
     * @param differ
     */
    protected void setPlanStatusFieldName(JsonDifferenceDTO differ) {
        ContractService contractService = CommonBeanFactory.getBean(ContractService.class);
        assert contractService != null;
        if (differ.getOldValue() != null) {
            String oldName = Translator.get("contract.payment_plan.status." + differ.getOldValue().toString().toLowerCase());
            differ.setOldValueName(oldName);
        }
        if (differ.getNewValue() != null) {
            String newName = Translator.get("contract.payment_plan.status." + differ.getNewValue().toString().toLowerCase());
            differ.setNewValueName(newName);
        }
    }
}