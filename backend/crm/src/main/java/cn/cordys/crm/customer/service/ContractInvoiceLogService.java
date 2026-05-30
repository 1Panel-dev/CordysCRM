package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.crm.contract.constants.ContractApprovalStatus;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractInvoiceLogService extends BaseModuleLogService {

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences = super.handleModuleLogField(differences, orgId, FormKey.INVOICE.getKey());

        for (JsonDifferenceDTO differ : differences) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.INVOICE_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
            } else if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.INVOICE_CONTRACT_ID.getBusinessKey())) {
                setContractFieldName(differ);
            } else if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.INVOICE_BUSINESS_TITLE_ID.getBusinessKey())) {
                setBusinessTitleName(differ);
            } else if (Strings.CI.equals(differ.getColumn(), "approvalStatus") && Arrays.stream(ContractApprovalStatus.values()).anyMatch(status -> status.name().equals(differ.getOldValue()))) {
                setApprovalName(differ);
            }
        }
        return differences;
    }
}