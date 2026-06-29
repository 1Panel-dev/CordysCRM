package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.EnableFieldValue;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.JsonDifferenceUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.constants.ApproverTypeEnum;
import cn.cordys.crm.approval.dto.ApprovalPostConfigDTO;
import cn.cordys.crm.approval.dto.WebHookConfig;
import cn.cordys.crm.approval.dto.response.ApprovalFlowDetailResponse;
import cn.cordys.crm.approval.dto.response.ApprovalFlowNodeConfigResponse;
import cn.cordys.crm.contract.service.ContractStageService;
import cn.cordys.crm.order.service.OrderStageService;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.mapper.ExtRoleMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.SysOperationLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class ApprovalFlowLogService extends BaseModuleLogService {

    private static final String NODES_COLUMN = "nodes";
    private static final String NULL_STRING = "null";

    private ApprovalFlowDetailResponse oldApprovalFlowDetail;
    private ApprovalFlowDetailResponse newApprovalFlowDetail;
    private List<BaseField> fields;
    private String formType;

    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private ExtRoleMapper extRoleMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {

        differences.removeIf(differ -> Strings.CS.equalsAny(differ.getColumn(), "currentVersionId", "links", "nodes",
                "optionMap", "createUserName", "updateUserName", "createNodeConfig", "updateNodeConfig", "deleteNodeConfig"));

        Map<String, Consumer<JsonDifferenceDTO>> handlers = Map.ofEntries(
                Map.entry("formType", this::handleFormType),
                Map.entry("enable", this::handleEnable),
                Map.entry("createExecute", this::handleBooleanValue),
                Map.entry("updateExecute", this::handleBooleanValue),
                Map.entry("deleteExecute", this::handleBooleanValue),
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

        if (isValidValue(oldValue) && isValidValue(newValue)) {
            oldApprovalFlowDetail = JSON.parseObject(oldValue, ApprovalFlowDetailResponse.class);
            newApprovalFlowDetail = JSON.parseObject(newValue, ApprovalFlowDetailResponse.class);
            formType = oldApprovalFlowDetail.getFormType();
            fields = moduleFormCacheService.getBusinessFormConfig(oldApprovalFlowDetail.getFormType(), orgId).getFields();
            List<JsonDifferenceDTO> createNodeDiffs = handleNodeConfigDifference(oldApprovalFlowDetail.getCreateNodeConfig(), newApprovalFlowDetail.getCreateNodeConfig(), Translator.get("opportunity.add"));
            List<JsonDifferenceDTO> updateNodeDiffs = handleNodeConfigDifference(oldApprovalFlowDetail.getUpdateNodeConfig(), newApprovalFlowDetail.getUpdateNodeConfig(), Translator.get("permission.edit"));
            List<JsonDifferenceDTO> deleteNodeDiffs = handleNodeConfigDifference(oldApprovalFlowDetail.getDeleteNodeConfig(), newApprovalFlowDetail.getDeleteNodeConfig(), Translator.get("permission.delete"));
            differences.addAll(createNodeDiffs);
            differences.addAll(updateNodeDiffs);
            differences.addAll(deleteNodeDiffs);
        }

        return differences;
    }

    private List<JsonDifferenceDTO> handleNodeConfigDifference(ApprovalFlowNodeConfigResponse oldCreateNodeConfig, ApprovalFlowNodeConfigResponse newNodeConfig, String prefix) {
        SysOperationLogService logService = CommonBeanFactory.getBean(SysOperationLogService.class);
        String oldStr = oldCreateNodeConfig == null ? "{}" : JSON.toJSONString(oldCreateNodeConfig);
        String newStr = newNodeConfig == null ? "{}" : JSON.toJSONString(newNodeConfig);
        try {
            List<JsonDifferenceDTO> configDifferences = logService.getJsonDifferences(oldStr, newStr);
            JsonDifferenceDTO nodesDiff = getNodesJsonDifferenceDTO(configDifferences, NODES_COLUMN);
            // 处理 nodes 字段的差异
            List<JsonDifferenceDTO> nodesDifference = handleNodesDifference(nodesDiff);
            nodesDifference.forEach(diff -> {
                diff.setColumnName(prefix + "-" + diff.getColumnName());
            });
            return nodesDifference;
        } catch (Exception e) {
           log.error(e.getMessage(), e);
        }
        return List.of();
    }

    /**
     * 处理 nodes 字段的差异
     * 按 number 分组比较节点属性，生成格式为 "流程设置-{number}-{属性名}" 的差异记录
     */
    private List<JsonDifferenceDTO> handleNodesDifference(JsonDifferenceDTO nodesDiff) {
        if (nodesDiff == null) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode oldNodes = nodesDiff.getOldValue() != null
                    ? mapper.valueToTree(nodesDiff.getOldValue())
                    : mapper.createArrayNode();
            JsonNode newNodes = nodesDiff.getNewValue() != null
                    ? mapper.valueToTree(nodesDiff.getNewValue())
                    : mapper.createArrayNode();

            // 按 number 分组
            Map<String, JsonNode> oldNodeMap = buildNodeMapByNumber(oldNodes);
            Map<String, JsonNode> newNodeMap = buildNodeMapByNumber(newNodes);

            // 找出所有涉及的 number
            Set<String> allNumbers = new LinkedHashSet<>();
            allNumbers.addAll(oldNodeMap.keySet());
            allNumbers.addAll(newNodeMap.keySet());

            List<JsonDifferenceDTO> nodeDifferences = new ArrayList<>();

            for (String number : allNumbers) {
                JsonNode oldNode = oldNodeMap.get(number);
                JsonNode newNode = newNodeMap.get(number);

                if (oldNode == null && newNode != null) {
                    // 新增节点
                    handleAddedNode(number, newNode, nodeDifferences);
                } else if (oldNode != null && newNode == null) {
                    // 删除节点
                    handleRemovedNode(number, oldNode, nodeDifferences);
                } else if (oldNode != null) {
                    // 修改节点，比较属性
                    compareNodeProperties(number, oldNode, newNode, nodeDifferences);
                }
            }

            return nodeDifferences;
        } catch (Exception e) {
            // 解析失败时忽略
        }
        return List.of();
    }

    private JsonDifferenceDTO getNodesJsonDifferenceDTO(List<JsonDifferenceDTO> differences, String nodeName) {
        Optional<JsonDifferenceDTO> nodesDiffOpt = differences.stream()
                .filter(differ -> Strings.CS.equals(differ.getColumn(), nodeName))
                .findFirst();

        return nodesDiffOpt.orElse(null);
    }

    /**
     * 按 number 构建节点映射
     */
    private Map<String, JsonNode> buildNodeMapByNumber(JsonNode nodes) {
        Map<String, JsonNode> nodeMap = new LinkedHashMap<>();
        if (nodes == null || !nodes.isArray()) {
            return nodeMap;
        }
        for (JsonNode node : nodes) {
            JsonNode numberNode = node.get("number");
            if (numberNode != null && StringUtils.isNotBlank(numberNode.asText())) {
                nodeMap.put(numberNode.asText(), node);
            }
        }
        return nodeMap;
    }

    /**
     * 处理新增节点
     */
    private void handleAddedNode(String number, JsonNode newNode, List<JsonDifferenceDTO> differences) {
        String nodeName = getNodeName(newNode);
        JsonDifferenceDTO diff = new JsonDifferenceDTO();
        diff.setColumnName(buildNodeColumnName(number, Translator.get("log.nodeName")));
        diff.setNewValue(nodeName);
        diff.setNewValueName(nodeName);
        diff.setType("add");
        differences.add(diff);
    }

    /**
     * 处理删除节点
     */
    private void handleRemovedNode(String number, JsonNode oldNode, List<JsonDifferenceDTO> differences) {
        String nodeName = getNodeName(oldNode);

        JsonDifferenceDTO diff = new JsonDifferenceDTO();
        diff.setColumnName(buildNodeColumnName(number, Translator.get("log.nodeName")));
        diff.setOldValue(nodeName);
        diff.setOldValueName(nodeName);
        diff.setType("removed");
        differences.add(diff);
    }

    /**
     * 比较节点属性
     */
    private void compareNodeProperties(String number, JsonNode oldNode, JsonNode newNode, List<JsonDifferenceDTO> differences) {
        // 需要比较的属性及其翻译 key
        Map<String, String> propertyNames = new LinkedHashMap<>();
        propertyNames.put("name", "log.nodeName");
        propertyNames.put("nodeType", "log.nodeType");
        propertyNames.put("approvalType", "log.approvalType");
        propertyNames.put("approverType", "log.approverType");
        propertyNames.put("approverList", "log.approverList");
        propertyNames.put("multiApproverMode", "log.multiApproverMode");
        propertyNames.put("emptyApproverAction", "log.emptyApproverAction");
        propertyNames.put("fallbackApprover", "log.fallbackApprover");
        propertyNames.put("sameSubmitterAction", "log.sameSubmitterAction");
        propertyNames.put("ccType", "log.ccType");
        propertyNames.put("ccList", "log.ccList");
        propertyNames.put("passPostConfig", "log.passPostConfig");
        propertyNames.put("rejectPostConfig", "log.rejectPostConfig");
        propertyNames.put("fieldPermissions", "log.fieldPermissions");
        propertyNames.put("conditionConfig", "log.conditionConfig");

        Set<String> comparedFields = new HashSet<>();

        // 比较旧节点中的属性
        Iterator<String> oldFields = oldNode.fieldNames();
        while (oldFields.hasNext()) {
            String field = oldFields.next();
            if (!propertyNames.containsKey(field) || comparedFields.contains(field)) {
                continue;
            }
            comparedFields.add(field);

            JsonNode oldValue = oldNode.get(field);
            JsonNode newValue = newNode.get(field);

            if (!newNode.has(field)) {
                // 属性被删除
                JsonDifferenceDTO diff = new JsonDifferenceDTO();
                diff.setColumnName(buildNodeColumnName(number, Translator.get(propertyNames.get(field))));
                diff.setOldValue(JsonDifferenceUtils.getValue(oldValue));
                diff.setType("removed");
                setNodePropertyValue(diff, field, oldNode, null, true, false);
                differences.add(diff);
            } else if (!isNodeEquals(oldValue, newValue)) {
                // 属性被修改
                JsonDifferenceDTO diff = new JsonDifferenceDTO();
                diff.setColumnName(buildNodeColumnName(number, Translator.get(propertyNames.get(field))));
                diff.setOldValue(JsonDifferenceUtils.getValue(oldValue));
                diff.setNewValue(JsonDifferenceUtils.getValue(newValue));
                diff.setType("modified");
                setNodePropertyValue(diff, field, oldNode, newNode, true, true);
                differences.add(diff);
            }
        }

        // 检查新增的属性
        Iterator<String> newFields = newNode.fieldNames();
        while (newFields.hasNext()) {
            String field = newFields.next();
            if (!propertyNames.containsKey(field) || comparedFields.contains(field)) {
                continue;
            }
            JsonNode newValue = newNode.get(field);
            JsonDifferenceDTO diff = new JsonDifferenceDTO();
            diff.setColumnName(buildNodeColumnName(number, Translator.get(propertyNames.get(field))));
            diff.setNewValue(JsonDifferenceUtils.getValue(newValue));
            diff.setType("add");
            setNodePropertyValue(diff, field, null, newNode, false, true);
            differences.add(diff);
        }
    }

    /**
     * 构建节点列名
     */
    private String buildNodeColumnName(String number, String propertyName) {
        return Translator.get("approval_flow.log.process_setup") + "-" + number + "-" + propertyName;
    }

    /**
     * 获取节点名称
     */
    private String getNodeName(JsonNode node) {
        JsonNode nameNode = node.get("name");
        return nameNode != null ? nameNode.asText() : "";
    }

    /**
     * 判断两个 JsonNode 是否相等
     */
    private boolean isNodeEquals(JsonNode oldValue, JsonNode newValue) {
        if (oldValue == null && newValue == null) {
            return true;
        }
        if (oldValue == null || newValue == null) {
            return false;
        }
        if (oldValue.isNumber() && newValue.isNumber()) {
            return oldValue.asDouble() == newValue.asDouble();
        }
        return oldValue.equals(newValue);
    }

    /**
     * 设置节点属性值的显示名称
     *
     * @param diff        差异对象
     * @param field       字段名
     * @param oldNode     旧节点数据（用于获取类型）
     * @param newNode     新节点数据（用于获取类型）
     * @param hasOldValue 是否有旧值
     * @param hasNewValue 是否有新值
     */
    private void setNodePropertyValue(JsonDifferenceDTO diff, String field, JsonNode oldNode, JsonNode newNode, boolean hasOldValue, boolean hasNewValue) {
        switch (field) {
            case "approvalType":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(Translator.get("approval_flow.approval_type." + diff.getOldValue().toString().toLowerCase()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(Translator.get("approval_flow.approval_type." + diff.getNewValue().toString().toLowerCase()));
                }
                break;
            case "approverType":
            case "ccType":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(Translator.get("approval_flow.approver_type." + diff.getOldValue().toString().toLowerCase()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(Translator.get("approval_flow.approver_type." + diff.getNewValue().toString().toLowerCase()));
                }
                break;
            case "multiApproverMode":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(Translator.get("approval_flow.multi_approver_mode." + diff.getOldValue().toString().toLowerCase()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(Translator.get("approval_flow.multi_approver_mode." + diff.getNewValue().toString().toLowerCase()));
                }
                break;
            case "emptyApproverAction":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(Translator.get("approval_flow.empty_approver_action." + diff.getOldValue().toString().toLowerCase()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(Translator.get("approval_flow.empty_approver_action." + diff.getNewValue().toString().toLowerCase()));
                }
                break;
            case "sameSubmitterAction":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(Translator.get("approval_flow.same_submitter_action." + diff.getOldValue().toString().toLowerCase()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(Translator.get("approval_flow.same_submitter_action." + diff.getNewValue().toString().toLowerCase()));
                }
                break;
            case "nodeType":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(Translator.get("approval_flow.node_type." + diff.getOldValue().toString().toLowerCase()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(Translator.get("approval_flow.node_type." + diff.getNewValue().toString().toLowerCase()));
                }
                break;
            case "approverList":
                if (hasOldValue && oldNode != null) {
                    String oldApproverType = getNodeFieldValue(oldNode, "approverType");
                    diff.setOldValueName(translateApproverList(diff.getOldValue(), oldApproverType));
                }
                if (hasNewValue && newNode != null) {
                    String newApproverType = getNodeFieldValue(newNode, "approverType");
                    diff.setNewValueName(translateApproverList(diff.getNewValue(), newApproverType));
                }
                break;
            case "ccList":
                if (hasOldValue && oldNode != null) {
                    String oldCcType = getNodeFieldValue(oldNode, "ccType");
                    diff.setOldValueName(translateApproverList(diff.getOldValue(), oldCcType));
                }
                if (hasNewValue && newNode != null) {
                    String newCcType = getNodeFieldValue(newNode, "ccType");
                    diff.setNewValueName(translateApproverList(diff.getNewValue(), newCcType));
                }
                break;
            case "fallbackApprover":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(translateMemberList(diff.getOldValue()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(translateMemberList(diff.getNewValue()));
                }
                break;
            case "passPostConfig":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(translatePostConfig(diff.getOldValue()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(translatePostConfig(diff.getNewValue()));
                }
                break;
            case "rejectPostConfig":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(translatePostConfig(diff.getOldValue()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(translatePostConfig(diff.getNewValue()));
                }
                break;
            case "fieldPermissions":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(translateFieldPermissions(diff.getOldValue()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(translateFieldPermissions(diff.getNewValue()));
                }
                break;
            case "conditionConfig":
                if (hasOldValue && isValidValue(diff.getOldValue())) {
                    diff.setOldValueName(translateConditionConfig(diff.getOldValue()));
                }
                if (hasNewValue && isValidValue(diff.getNewValue())) {
                    diff.setNewValueName(translateConditionConfig(diff.getNewValue()));
                }
                break;
            default:
                if (hasOldValue) {
                    diff.setOldValueName(diff.getOldValue());
                }
                if (hasNewValue) {
                    diff.setNewValueName(diff.getNewValue());
                }
        }
    }

    /**
     * 获取节点字段值
     */
    private String getNodeFieldValue(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode fieldNode = node.get(field);
        return fieldNode != null ? fieldNode.asText() : null;
    }

    /**
     * 判断值是否有效（不为 null 且不为 "null" 字符串）
     */
    private boolean isValidValue(Object value) {
        return value != null && !NULL_STRING.equals(value.toString());
    }

    /**
     * 翻译审批人/抄送人列表
     * 根据类型将列表值翻译为可读文本
     */
    private String translateApproverList(Object value, String approverType) {
        if (value == null || !isValidValue(approverType)) {
            if (value instanceof List<?>) {
                return null;
            }
            return value != null ? value.toString() : "";
        }

        // 如果是层级类型（上级、部门负责人），翻译层级数字
        if (ApproverTypeEnum.SUPERIOR.name().equalsIgnoreCase(approverType)
                || ApproverTypeEnum.MULTIPLE_SUPERIOR.name().equalsIgnoreCase(approverType)) {
            return translateLevelList(value, "approval_flow.level.supervisor.");
        }
        if (ApproverTypeEnum.DEPT_HEAD.name().equalsIgnoreCase(approverType)
                || ApproverTypeEnum.MULTIPLE_DEPT_HEAD.name().equalsIgnoreCase(approverType)) {
            return translateLevelList(value, "approval_flow.level.department.");
        }

        // 成员类型，查询用户名称
        if (ApproverTypeEnum.MEMBER.name().equalsIgnoreCase(approverType)) {
            return translateMemberList(value);
        }

        // 角色类型，查询角色名称
        if (ApproverTypeEnum.ROLE.name().equalsIgnoreCase(approverType)) {
            return translateRoleList(value);
        }

        if (value instanceof List<?>) {
            return null;
        }

        // 其他类型，直接返回原值
        return value.toString();
    }

    /**
     * 翻译成员列表（ID -> 名称）
     * 支持单个用户ID字符串或用户ID列表
     */
    private String translateMemberList(Object value) {
        List<String> userIds = parseStringList(value);
        // 如果不是列表格式，尝试作为单个ID处理
        if (userIds.isEmpty() && value != null && !NULL_STRING.equals(value.toString())) {
            String singleId = value.toString();
            if (StringUtils.isNotBlank(singleId)) {
                userIds = List.of(singleId);
            }
        }
        if (userIds.isEmpty()) {
            return "";
        }
        List<OptionDTO> userOptions = extUserMapper.selectUserOptionByIds(userIds);
        if (userOptions.isEmpty()) {
            return value.toString();
        }
        // 按 userIds 顺序返回名称
        Map<String, String> nameMap = userOptions.stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName, (a, b) -> a));
        return userIds.stream()
                .map(nameMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    /**
     * 翻译成员列表（ID -> 名称）
     * 支持单个用户ID字符串或用户ID列表
     */
    private String translateDepartmentList(Object value) {
        List<String> deptIds = parseStringList(value);
        // 如果不是列表格式，尝试作为单个ID处理
        if (deptIds.isEmpty() && value != null && !NULL_STRING.equals(value.toString())) {
            String singleId = value.toString();
            if (StringUtils.isNotBlank(singleId)) {
                deptIds = List.of(singleId);
            }
        }
        if (deptIds.isEmpty()) {
            return "";
        }
        return extDepartmentMapper.getNameByIds(deptIds).stream()
                .collect(Collectors.joining(", "));
    }

    /**
     * 翻译角色列表（ID -> 名称）
     * 支持单个角色ID字符串或角色ID列表
     */
    private String translateRoleList(Object value) {
        List<String> roleIds = parseStringList(value);
        // 如果不是列表格式，尝试作为单个ID处理
        if (roleIds.isEmpty() && value != null && !NULL_STRING.equals(value.toString())) {
            String singleId = value.toString();
            if (StringUtils.isNotBlank(singleId)) {
                roleIds = List.of(singleId);
            }
        }
        if (roleIds.isEmpty()) {
            return "";
        }
        List<OptionDTO> roleOptions = extRoleMapper.getIdNameByIds(roleIds);
        if (roleOptions.isEmpty()) {
            return value.toString();
        }
        // 按 roleIds 顺序返回名称
        Map<String, String> nameMap = roleOptions.stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName, (a, b) -> a));
        return roleIds.stream()
                .map(nameMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    /**
     * 解析字符串列表
     */
    private List<String> parseStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        try {
            if (value instanceof List<?> list) {
                return list.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .filter(s -> !NULL_STRING.equals(s))
                        .collect(Collectors.toList());
            }
            String strValue = value.toString();
            if (strValue.startsWith("[")) {
                List<?> list = JSON.parseArray(strValue, Object.class);
                return list.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .filter(s -> !NULL_STRING.equals(s))
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
            // 解析失败时返回空列表
        }
        return List.of();
    }

    /**
     * 翻译层级列表
     */
    private String translateLevelList(Object value, String translationPrefix) {
        if (value == null) {
            return "";
        }
        // 尝试解析为列表
        try {
            if (value instanceof List<?> list) {
                List<String> translated = new ArrayList<>();
                for (Object item : list) {
                    String levelKey = translationPrefix + item.toString();
                    translated.add(Translator.get(levelKey));
                }
                return String.join(", ", translated);
            }
            // 如果是 JSON 字符串
            String strValue = value.toString();
            if (strValue.startsWith("[")) {
                List<?> list = JSON.parseArray(strValue, Object.class);
                List<String> translated = new ArrayList<>();
                for (Object item : list) {
                    String levelKey = translationPrefix + item.toString();
                    translated.add(Translator.get(levelKey));
                }
                return String.join(", ", translated);
            }
        } catch (Exception ignored) {
            // 解析失败时返回原值
        }
        return value.toString();
    }

    private void handleFormType(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.formType"));
        if (isValidValue(differ.getOldValue())) {
            differ.setOldValueName(Translator.get("approval_flow.form_type." + differ.getOldValue().toString().toLowerCase()));
        }
        if (isValidValue(differ.getNewValue())) {
            differ.setNewValueName(Translator.get("approval_flow.form_type." + differ.getNewValue().toString().toLowerCase()));
        }
    }

    private void handleEnable(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.enable"));
        if (isValidValue(differ.getOldValue())) {
            differ.setOldValueName(Translator.get("approval_flow.enable." + (Boolean.parseBoolean(differ.getOldValue().toString()) ? "true" : "false")));
        }
        if (isValidValue(differ.getNewValue())) {
            differ.setNewValueName(Translator.get("approval_flow.enable." + (Boolean.parseBoolean(differ.getNewValue().toString()) ? "true" : "false")));
        }
    }

    private void handleBooleanValue(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log." + differ.getColumn()));
        if (isValidValue(differ.getOldValue())) {
            differ.setOldValueName(Translator.get("log.enable." + (Boolean.parseBoolean(differ.getOldValue().toString()) ? "true" : "false")));
        }
        if (isValidValue(differ.getNewValue())) {
            differ.setNewValueName(Translator.get("log.enable." + (Boolean.parseBoolean(differ.getNewValue().toString()) ? "true" : "false")));
        }
    }

    private void handleDuplicateApproverRule(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.duplicateApproverRule"));
        if (isValidValue(differ.getOldValue())) {
            differ.setOldValueName(Translator.get("approval_flow.duplicate_approver_rule." + differ.getOldValue().toString().toLowerCase()));
        }
        if (isValidValue(differ.getNewValue())) {
            differ.setNewValueName(Translator.get("approval_flow.duplicate_approver_rule." + differ.getNewValue().toString().toLowerCase()));
        }
    }

    private void handleStatusPermissions(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("log.statusPermissions"));
        if (isValidValue(differ.getOldValue())) {
            differ.setOldValueName(translateStatusPermissions(differ.getOldValue()));
        }
        if (isValidValue(differ.getNewValue())) {
            differ.setNewValueName(translateStatusPermissions(differ.getNewValue()));
        }
    }

    /**
     * 翻译状态权限配置
     * 将 [{"approvalStatus":"REVOKED","permission":"ORDER:READ","enabled":true}]
     * 翻译为换行分隔的字符串
     */
    @SuppressWarnings("unchecked")
    private String translateStatusPermissions(Object value) {
        if (value == null) {
            return "";
        }
        try {
            List<?> list;
            if (value instanceof List<?> l) {
                list = l;
            } else {
                String strValue = value.toString();
                list = JSON.parseArray(strValue, Object.class);
            }

            List<String> results = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String enabled = map.get("enabled") != null ? map.get("enabled").toString() : "";
                    if (Strings.CI.equals(enabled, "true")) {
                        String status = map.get("approvalStatus") != null ? map.get("approvalStatus").toString() : "";
                        String permission = map.get("permission") != null ? map.get("permission").toString() : "";
                        String translatedStatus = translateApprovalStatus(status);
                        String translatedPermission = translatePermission(permission);
                        results.add(translatedStatus + "-" + translatedPermission);
                    }
                }
            }

            return String.join("\n", results);
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 翻译审批状态
     */
    private String translateApprovalStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return "";
        }
        return Translator.get("log.approvalStatus." + status, status);
    }

    /**
     * 翻译权限字符串
     * 将 "ORDER:READ" 翻译为 "订单:查看"
     */
    private String translatePermission(String permission) {
        if (StringUtils.isBlank(permission)) {
            return "";
        }
        String[] parts = permission.split(":");
        if (parts.length >= 2) {
            String moduleKey = "permission." + parts[0].toLowerCase() + ".name";
            String permissionKey = "permission." + parts[1].toLowerCase();
            return Translator.get(moduleKey, parts[0]) + ":" + Translator.get(permissionKey, parts[1]);
        }
        return Translator.get("permission." + permission.toLowerCase(), permission);
    }

    private void handleDescription(JsonDifferenceDTO differ) {
        differ.setColumnName(Translator.get("approval_flow.log.description"));
        differ.setOldValueName(differ.getOldValue());
        differ.setNewValueName(differ.getNewValue());
    }

    /**
     * 翻译审批通过/驳回后配置
     * 将 fieldUpdateConfigs 翻译为 "字段名: 值 (启用/禁用)" 格式
     */
    private String translatePostConfig(Object value) {
        if (value == null) return "";
        try {
            String jsonStr = value instanceof String ? (String) value : JSON.toJSONString(value);
            ApprovalPostConfigDTO postConfig = JSON.parseObject(jsonStr, ApprovalPostConfigDTO.class);

            if (postConfig == null) {
                return "";
            }

            Map<String, BaseField> fieldMap = fields != null
                    ? fields.stream().collect(Collectors.toMap(BaseField::getId, f -> f, (a, b) -> a))
                    : Map.of();

            List<String> parts = new ArrayList<>();
            for (EnableFieldValue config : postConfig.getFieldUpdateConfigs()) {
                if (config.getFieldId() == null) continue;

                BaseField field = fieldMap.get(config.getFieldId());
                String fieldName = field != null ? field.getName() : config.getFieldId();

                Object translatedValue = config.getFieldValue();
                if (field != null && config.getFieldValue() != null) {
                    translatedValue = transformFieldValue(field, config.getFieldValue());
                }

                String enableStr = BooleanUtils.isTrue(config.getEnable())
                        ? Translator.get("log.enable.true")
                        : Translator.get("log.enable.false");
                parts.add(fieldName + ": " + translatedValue + " (" + enableStr + ")");
            }
            if (postConfig.getWebHookConfig() != null) {
                WebHookConfig webHookConfig = postConfig.getWebHookConfig();
                parts.add(Translator.get("webhook.enable") + ": " + (BooleanUtils.isTrue(webHookConfig.getWebHookEnable()) ? Translator.get("log.enable.true") : Translator.get("log.enable.false")));
                parts.add(Translator.get("webhook.describe") + ": " + webHookConfig.getWebHookDescribe());
                parts.add(Translator.get("webhook.url") + ": " + webHookConfig.getWebHookUrl());
                parts.add(Translator.get("webhook.method") + ": " + webHookConfig.getWebHookMethod());
                parts.add(Translator.get("webhook.header") + ": " + webHookConfig.getWebHookHeader());
                parts.add(Translator.get("webhook.body") + ": " + webHookConfig.getWebHookBody());
            }

            return String.join("\n", parts);
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 翻译条件配置
     * 将 conditionConfig 翻译为 "字段名-操作符" 格式，如 [名称-新值不等于旧值，创建时间-动态-本周]
     */
    private String translateConditionConfig(Object value) {
        if (value == null) return "";
        try {
            String jsonStr = value instanceof String ? (String) value : JSON.toJSONString(value);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode configNode = mapper.readTree(jsonStr);

            JsonNode conditionsNode = configNode.get("conditions");
            if (conditionsNode == null || !conditionsNode.isArray() || conditionsNode.isEmpty()) {
                return "";
            }

            Map<String, BaseField> fieldMap = new HashMap<>();
            fields.forEach(field -> {
                fieldMap.put(field.getId(), field);
                if (StringUtils.isNotBlank(field.getBusinessKey())) {
                    fieldMap.put(field.getBusinessKey(), field);
                }
            });

            List<String> conditionParts = new ArrayList<>();
            for (JsonNode condition : conditionsNode) {
                String nameValue = condition.has("name") ? condition.get("name").asText() : "";
                String operator = condition.has("operator") ? condition.get("operator").asText() : "";

                String fieldName = translateConditionFieldName(nameValue, fieldMap);
                String operatorName = Translator.get("approval_flow.condition.operator." + operator.toLowerCase(), operator);

                StringBuilder sb = new StringBuilder();
                sb.append(fieldName).append("-").append(operatorName);

                if ("DYNAMICS".equals(operator) && condition.has("value") && !condition.get("value").isNull()) {
                    String dynamicValue = condition.get("value").asText();
                    String dynamicName = translateDynamicTimeValue(dynamicValue);
                    sb.append("-").append(dynamicName);
                } else if (!Strings.CS.equalsAny(operator, "NOT_EQUAL_ORIGINAL", "EMPTY", "NOT_EMPTY")
                        && condition.has("value") && !condition.get("value").isNull()) {
                    String translatedValue = translateConditionValue(condition.get("value"), nameValue, fieldMap);
                    if (StringUtils.isNotBlank(translatedValue)) {
                        sb.append("-").append(translatedValue);
                    }
                }

                conditionParts.add(sb.toString());
            }

            return "[" + String.join("，", conditionParts) + "]";
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 翻译条件字段名
     * 先按字段ID从业务字段中查找，再通过BusinessModuleField匹配businessKey翻译，最后尝试i18n
     */
    private String translateConditionFieldName(String name, Map<String, BaseField> fieldMap) {
        BaseField field = fieldMap.get(name);
        if (field != null) {
            return field.getName();
        }
        // 通过 businessKey 查找 BusinessModuleField 获取 i18n key
        if (formType != null) {
            for (BusinessModuleField bmf : BusinessModuleField.values()) {
                if (bmf.getBusinessKey().equals(name) && formType.equals(bmf.getFormKey())) {
                    return Translator.get(bmf.getKey(), name);
                }
            }
            if (formType.equals(FormKey.ORDER.getKey()) && Strings.CI.equals(name, "stage")) {
                return Translator.get("order.stage", name);
            }
        }
        return Translator.get("approval_flow.condition.field." + name, name);
    }

    /**
     * 翻译动态时间值
     * 支持单值（WEEK, MONTH等）和自定义格式（0,1,BEFORE_DAY等）
     */
    private String translateDynamicTimeValue(String value) {
        if (value == null || value.isEmpty()) return "";
        String[] parts = value.split(",");
        if (parts.length == 1) {
            return Translator.get("approval_flow.condition.dynamics." + parts[0].toLowerCase(), parts[0]);
        } else if (parts.length >= 3) {
            String dateNumber = parts[1];
            String dateUnit = parts[2];
            String unitName = Translator.get("approval_flow.condition.dynamics." + dateUnit.toLowerCase(), dateUnit);
            return dateNumber + unitName;
        } else if (parts.length == 2) {
            String dateNumber = parts[0];
            String unitName = Translator.get("approval_flow.condition.dynamics." + parts[1].toLowerCase(), parts[1]);
            return dateNumber + unitName;
        }
        return value;
    }

    /**
     * 翻译条件值
     * 根据字段类型将值翻译为可读文本（用户ID→名称、数据源ID→名称、选项值→标签等）
     * 单选类型字段且值为数组时，循环匹配选项标签；系统字段按businessKey类型翻译
     */
    private String translateConditionValue(JsonNode valueNode, String fieldName, Map<String, BaseField> fieldMap) {
        BaseField field = fieldMap.get(fieldName);
        if (field != null) {
            try {
                if (valueNode.isArray() && field.hasSingleOptions()) {
                    List<String> labels = new ArrayList<>();
                    for (String item : JSON.parseArray(valueNode.toString(), String.class)) {
                        Object translated = transformFieldValue(field, item);
                        if (translated instanceof String translatedStr) {
                            labels.add(translatedStr);
                        }
                    }
                    return labels.stream().collect(Collectors.joining(", "));
                } else {
                    Object translated = transformFieldValue(field, valueNode);
                    return translated != null ? translated.toString() : "";
                }
            } catch (Exception e) {
                return valueNode.asText("");
            }
        }
        // 系统字段值翻译：根据 businessKey 判断字段类型
        if (valueNode.isNull()) {
            return "";
        }
        // owner → 用户名称
        if ("owner".equals(fieldName)) {
            return translateMemberList(valueNode.asText(""));
        }
        if (fieldName.toLowerCase().contains("user")) {
            return translateMemberList(valueNode);
        }
        if (fieldName.contains("department")) {
            return translateDepartmentList(valueNode);
        }
        // stage → 阶段名称
        if ("stage".equals(fieldName)) {
            return translateStageValue(valueNode);
        }
        if ("invalid".equals(fieldName)) {
            return translateInvalidValue(valueNode);
        }
        // approvalStatus → 审批状态
        if ("approvalStatus".equals(fieldName)) {
            return Translator.get("approval_flow.condition.approval_status." + valueNode.asText("").toLowerCase(), valueNode.asText(""));
        }

        if (valueNode.isArray()) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : valueNode) {
                items.add(item.asText(""));
            }
            return items.stream().collect(Collectors.joining(", "));
        }
        return valueNode.asText("");
    }

    /**
     * 翻译阶段值
     * 支持单个阶段ID字符串或阶段ID数组
     */
    private String translateStageValue(Object value) {
        List<String> stageIds = parseStringList(value);
        if (stageIds.isEmpty() && value != null && !NULL_STRING.equals(value.toString())) {
            String singleId = value.toString();
            if (StringUtils.isNotBlank(singleId)) {
                stageIds = List.of(singleId);
            }
        }
        if (stageIds.isEmpty()) {
            return "";
        }
        try {
            OrderStageService orderStageService = CommonBeanFactory.getBean(OrderStageService.class);
            ContractStageService contractStageService = CommonBeanFactory.getBean(ContractStageService.class);
            List<StageConfigResponse> stages;
            if (formType != null && (formType.equals(FormKey.ORDER.getKey()))) {
                stages = orderStageService != null ? orderStageService.getStageConfigList(OrganizationContext.getOrganizationId()).getStageConfigList() : List.of();
            } else if (formType != null && formType.equals(FormKey.CONTRACT.getKey())) {
                stages = contractStageService != null ? contractStageService.getStageConfigList(OrganizationContext.getOrganizationId()).getStageConfigList() : List.of();
            } else {
                return String.join(", ", stageIds);
            }
            Map<String, String> stageMap = stages.stream()
                    .collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName, (a, b) -> a));
            return stageIds.stream()
                    .map(id -> stageMap.getOrDefault(id, id))
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return String.join(", ", stageIds);
        }
    }

    /**
     * 翻译作废状态值
     * Boolean 或 Boolean 数组，true → 作废，false → 正常
     */
    private String translateInvalidValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return "";
        }
        if (valueNode.isArray()) {
            List<String> results = new ArrayList<>();
            for (JsonNode item : valueNode) {
                results.add(Translator.get("approval_flow.invalid." + item.asBoolean()));
            }
            return String.join(", ", results);
        }
        return Translator.get("approval_flow.invalid." + valueNode.asBoolean());
    }

    private String translateFieldPermissions(Object value) {
        if (value == null) return "";
        try {
            List<?> list;
            if (value instanceof List<?> l) {
                list = l;
            } else {
                String strValue = value.toString();
                list = JSON.parseArray(strValue, Object.class);
            }

            Map<String, BaseField> fieldMap = fields != null
                    ? fields.stream().collect(Collectors.toMap(BaseField::getId, f -> f, (a, b) -> a))
                    : Map.of();

            List<String> parts = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String fieldId = map.get("fieldId") != null ? map.get("fieldId").toString() : "";
                    String permissionType = map.get("permissionType") != null ? map.get("permissionType").toString() : "";

                    BaseField field = fieldMap.get(fieldId);
                    String fieldName = field != null ? field.getName() : fieldId;
                    String translatedPermission = Translator.get("approval_flow.field_permission." + permissionType.toLowerCase(), permissionType);

                    parts.add(fieldName + ": " + translatedPermission);
                }
            }

            return String.join("\n", parts);
        } catch (Exception e) {
            return value.toString();
        }
    }
}