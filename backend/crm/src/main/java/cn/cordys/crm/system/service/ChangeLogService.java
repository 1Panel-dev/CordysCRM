package cn.cordys.crm.system.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.OperationLogBlob;
import cn.cordys.crm.system.dto.response.ChangeLogResponse;
import cn.cordys.crm.system.mapper.ExtOperationLogMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 详情页变更记录查询。
 * <p>
 * 变更记录即资源的操作日志，按表单类型映射到对应日志模块后按资源 ID 查询。
 * </p>
 */
@Service
@Slf4j
public class ChangeLogService {

    private static final List<String> CUSTOM_FORM_MODULES = List.of(LogModule.CUSTOM_FORM_DATA);

    private static final Map<String, List<String>> FORM_KEY_MODULES = Map.of(
            FormKey.CUSTOMER.getKey(), List.of(LogModule.CUSTOMER_INDEX, LogModule.CUSTOMER_POOL),
            FormKey.CLUE.getKey(), List.of(LogModule.CLUE_INDEX, LogModule.CLUE_POOL_INDEX),
            FormKey.CONTACT.getKey(), List.of(LogModule.CUSTOMER_CONTACT),
            FormKey.OPPORTUNITY.getKey(), List.of(LogModule.OPPORTUNITY_INDEX),
            FormKey.QUOTATION.getKey(), List.of(LogModule.OPPORTUNITY_QUOTATION),
            FormKey.CONTRACT.getKey(), List.of(LogModule.CONTRACT_INDEX),
            FormKey.CONTRACT_PAYMENT_PLAN.getKey(), List.of(LogModule.CONTRACT_PAYMENT),
            FormKey.CONTRACT_PAYMENT_RECORD.getKey(), List.of(LogModule.CONTRACT_PAYMENT_RECORD),
            FormKey.INVOICE.getKey(), List.of(LogModule.CONTRACT_INVOICE),
            FormKey.ORDER.getKey(), List.of(LogModule.ORDER_INDEX));

    @Resource
    private ExtOperationLogMapper extOperationLogMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<OperationLogBlob> operationLogBlobMapper;
    @Resource
    private SysOperationLogService sysOperationLogService;

    /**
     * 查询指定资源在对应表单下的变更记录。
     *
     * @param formKey    表单标识，自定义表单传表单 ID
     * @param resourceId 资源 ID
     * @param orgId      组织 ID
     */
    public List<ChangeLogResponse> list(String formKey, String resourceId, String orgId) {
        List<String> modules = FORM_KEY_MODULES.getOrDefault(formKey, CUSTOM_FORM_MODULES);
        List<ChangeLogResponse> list = extOperationLogMapper.listChangeLog(modules, resourceId, orgId);
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        fillOperatorNames(list);
        fillDiffs(list, orgId);
        return list;
    }

    private void fillOperatorNames(List<ChangeLogResponse> list) {
        List<String> userIds = list.stream()
                .map(ChangeLogResponse::getOperator)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        Map<String, String> userMap = extUserMapper.selectUserOptionByIds(userIds).stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
        list.forEach(item -> item.setOperatorName(userMap.getOrDefault(item.getOperator(), StringUtils.EMPTY)));
    }

    private void fillDiffs(List<ChangeLogResponse> list, String orgId) {
        List<String> ids = list.stream().map(ChangeLogResponse::getId).toList();
        Map<String, OperationLogBlob> blobMap = operationLogBlobMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(OperationLogBlob::getId, Function.identity()));
        for (ChangeLogResponse item : list) {
            OperationLogBlob blob = blobMap.get(item.getId());
            if (blob == null) {
                continue;
            }
            String oldValue = toJsonString(blob.getOriginalValue());
            String newValue = toJsonString(blob.getModifiedValue());
            if (StringUtils.isBlank(oldValue) && StringUtils.isBlank(newValue)) {
                continue;
            }
            try {
                List<JsonDifferenceDTO> differences = sysOperationLogService.getJsonDifferences(oldValue, newValue);
                if (CollectionUtils.isEmpty(differences)) {
                    continue;
                }
                BaseModuleLogService moduleLogService = ModuleLogServiceFactory.getModuleLogService(item.getModule());
                if (moduleLogService != null) {
                    moduleLogService.setOldValue(oldValue);
                    moduleLogService.setNewValue(newValue);
                    differences = moduleLogService.handleLogField(differences, orgId);
                } else {
                    differences.forEach(BaseModuleLogService::translatorDifferInfo);
                }
                item.setDiffs(Objects.requireNonNullElse(differences, List.of()));
            } catch (Exception e) {
                // 单条日志数据异常不应影响整页变更记录的展示
                log.warn("解析变更记录差异失败, logId: {}, message: {}", item.getId(), e.getMessage());
            }
        }
    }

    private String toJsonString(byte[] value) {
        return value == null ? StringUtils.EMPTY : new String(value, StandardCharsets.UTF_8);
    }
}
