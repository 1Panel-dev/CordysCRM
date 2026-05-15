package cn.cordys.crm.approval.service;

import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalFlowLogService extends BaseModuleLogService {

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        // 去掉 currentVersionId 字段
        differences.removeIf(differ -> Strings.CS.equalsAny(differ.getColumn(), "currentVersionId", "links", "nodes"));

        Map<String, Consumer<JsonDifferenceDTO>> handlers = Map.ofEntries(
                Map.entry("formType", this::handleFormType),
                Map.entry("enable", this::handleEnable),
                Map.entry("createExecute", this::handleBooleanValue),
                Map.entry("updateExecute", this::handleBooleanValue),
                Map.entry("submitterCanRevoke", this::handleBooleanValue),
                Map.entry("allowBatchProcess", this::handleBooleanValue),
                Map.entry("allowWithdraw", this::handleBooleanValue),
                Map.entry("allowAddSign", this::handleBooleanValue),
                Map.entry("requireComment", this::handleBooleanValue),
                Map.entry("duplicateApproverRule", this::handleDuplicateApproverRule),
                Map.entry("description", this::handleDescription),
                Map.entry("statusPermissions", this::handleStatusPermissions)
        );

        differences.forEach(differ -> {
            Consumer<JsonDifferenceDTO> handler = handlers.get(differ.getColumn());
            if (handler != null) {
                handler.accept(differ);
            } else {
                translatorDifferInfo(differ);
            }
        });
        return differences;
    }

    private void handleFormType(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.formType"));
        if (differ.getOldValue() != null) {
            differ.setOldValueName(Translator.get("approval_flow.form_type." + differ.getOldValue().toString().toLowerCase()));
        }
        if (differ.getNewValue() != null) {
            differ.setNewValueName(Translator.get("approval_flow.form_type." + differ.getNewValue().toString().toLowerCase()));
        }
    }

    private void handleEnable(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.enable"));
        if (differ.getOldValue() != null) {
            differ.setOldValueName(Translator.get("approval_flow.enable." + (Boolean.parseBoolean(differ.getOldValue().toString()) ? "true" : "false")));
        }
        if (differ.getNewValue() != null) {
            differ.setNewValueName(Translator.get("approval_flow.enable." + (Boolean.parseBoolean(differ.getNewValue().toString()) ? "true" : "false")));
        }
    }

    private void handleBooleanValue(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log." + differ.getColumn()));
        if (differ.getOldValue() != null) {
            differ.setOldValueName(Translator.get("log.enable." + (Boolean.parseBoolean(differ.getOldValue().toString()) ? "true" : "false")));
        }
        if (differ.getNewValue() != null) {
            differ.setNewValueName(Translator.get("log.enable." + (Boolean.parseBoolean(differ.getNewValue().toString()) ? "true" : "false")));
        }
    }

    private void handleDuplicateApproverRule(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.duplicateApproverRule"));
        if (differ.getOldValue() != null) {
            differ.setOldValueName(Translator.get("approval_flow.duplicate_approver_rule." + differ.getOldValue().toString().toLowerCase()));
        }
        if (differ.getNewValue() != null) {
            differ.setNewValueName(Translator.get("approval_flow.duplicate_approver_rule." + differ.getNewValue().toString().toLowerCase()));
        }
    }

    private void handleStatusPermissions(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.statusPermissions"));
        if (differ.getOldValue() != null ) {
            differ.setOldValueName(JSON.toJSONString(differ.getOldValue()));
        }
        if (differ.getNewValue() != null) {
            differ.setNewValueName(JSON.toJSONString(differ.getNewValue()));
        }
    }

    private void handleDescription(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("approval_flow.log.description"));
        differ.setOldValueName(differ.getOldValue());
        differ.setNewValueName(differ.getNewValue());
    }
}