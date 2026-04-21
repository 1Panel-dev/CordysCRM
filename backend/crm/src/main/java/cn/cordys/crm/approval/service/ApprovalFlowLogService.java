package cn.cordys.crm.approval.service;

import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalFlowLogService extends BaseModuleLogService {

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences.forEach(differ -> {
            switch (differ.getColumn()) {
                case "formType":
                    handleFormTypeLogDetail(differ);
                    break;
                case "executeTiming":
                    handleExecuteTimingLogDetail(differ);
                    break;
                case "enable":
                    handleEnableLogDetail(differ);
                    break;
                case "duplicateApproverRule":
                    handleDuplicateApproverRuleLogDetail(differ);
                    break;
                case "description":
                    differ.setColumnName(Translator.get("approval_flow.log.description"));
                    differ.setOldValueName(differ.getOldValue());
                    differ.setNewValueName(differ.getNewValue());
                    break;
                default:
                    BaseModuleLogService.translatorDifferInfo(differ);
            }
        });
        return differences;
    }

    private void handleFormTypeLogDetail(JsonDifferenceDTO differ) {
        if (differ.getOldValue() != null) {
            differ.setOldValueName(Translator.get("approval_flow.form_type." + differ.getOldValue().toString().toLowerCase()));
        }
        if (differ.getNewValue() != null) {
            differ.setNewValueName(Translator.get("approval_flow.form_type." + differ.getNewValue().toString().toLowerCase()));
        }
        differ.setColumnName(Translator.get("log.formType"));
    }

    private void handleExecuteTimingLogDetail(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.executeTiming"));
        differ.setOldValueName(differ.getOldValue());
        differ.setNewValueName(differ.getNewValue());
    }

    private void handleEnableLogDetail(JsonDifferenceDTO differ) {
        if (differ.getOldValue() != null) {
            differ.setOldValueName(Translator.get("approval_flow.enable." + (Boolean.parseBoolean(differ.getOldValue().toString()) ? "true" : "false")));
        }
        if (differ.getNewValue() != null) {
            differ.setNewValueName(Translator.get("approval_flow.enable." + (Boolean.parseBoolean(differ.getNewValue().toString()) ? "true" : "false")));
        }
        differ.setColumnName(Translator.get("log.enable"));
    }

    private void handleDuplicateApproverRuleLogDetail(JsonDifferenceDTO differ) {
        if (differ.getOldValue() != null) {
            differ.setOldValueName(Translator.get("approval_flow.duplicate_approver_rule." + differ.getOldValue().toString().toLowerCase()));
        }
        if (differ.getNewValue() != null) {
            differ.setNewValueName(Translator.get("approval_flow.duplicate_approver_rule." + differ.getNewValue().toString().toLowerCase()));
        }
        differ.setColumnName(Translator.get("log.duplicateApproverRule"));
    }
}