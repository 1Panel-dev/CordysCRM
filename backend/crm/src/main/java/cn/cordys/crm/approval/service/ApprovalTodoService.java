package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalState;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import cn.cordys.crm.approval.dto.response.ApprovalTodoResponse;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractInvoice;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.system.domain.User;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApprovalTodoService {

    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<OpportunityQuotation> quotationMapper;
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private BaseMapper<Order> orderMapper;
    @Resource
    private BaseMapper<ContractInvoice> invoiceMapper;

    public ApprovalTodoResponse getTodoList(String userId) {
        // 初始化分类响应对象。
        ApprovalTodoResponse response = initResponse();
        if (StringUtils.isBlank(userId)) {
            return response;
        }

        // 查询当前用户待审批任务。
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getApproverId, userId)
                .eq(ApprovalTask::getTaskStatus, ApprovalState.PENDING.getId());
        List<ApprovalTask> tasks = approvalTaskMapper.selectListByLambda(taskWrapper);
        if (tasks.isEmpty()) {
            return response;
        }

        // 收集任务对应审批实例ID。
        List<String> instanceIds = tasks.stream()
                .map(ApprovalTask::getInstanceId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (instanceIds.isEmpty()) {
            return response;
        }

        // 批量查询审批实例并建立映射。
        Map<String, ApprovalInstance> instanceMap = approvalInstanceMapper.selectByIds(instanceIds).stream()
                .collect(Collectors.toMap(ApprovalInstance::getId, Function.identity(), (prev, next) -> prev));
        if (instanceMap.isEmpty()) {
            return response;
        }

        // 仅保留当前节点上的待审批任务，避免混入历史节点任务。
        List<ApprovalTask> currentNodeTasks = tasks.stream()
                .filter(task -> {
                    ApprovalInstance instance = instanceMap.get(task.getInstanceId());
                    return instance != null && StringUtils.equals(instance.getCurrentNodeId(), task.getNodeId());
                })
                .toList();
        if (currentNodeTasks.isEmpty()) {
            return response;
        }

        // 预加载申请人名称和资源名称映射，减少循环内查询。
        List<ApprovalInstance> instances = currentNodeTasks.stream()
                .map(task -> instanceMap.get(task.getInstanceId()))
                .filter(Objects::nonNull)
                .toList();
        Map<String, String> submitterMap = loadSubmitterNameMap(instances);
        Map<ApprovalFormTypeEnum, Map<String, String>> resourceNameMap = loadResourceNameMap(instances);

        // 逐条组装待办并按资源类型归类返回。
        Set<String> processedInstanceIds = new HashSet<>();
        for (ApprovalTask task : currentNodeTasks) {
            // 同一实例只返回一条代办记录。
            if (!processedInstanceIds.add(task.getInstanceId())) {
                continue;
            }
            ApprovalInstance instance = instanceMap.get(task.getInstanceId());
            // 跳过异常实例数据。
            if (instance == null) {
                continue;
            }
            ApprovalFormTypeEnum formType = parseFormType(instance.getType());
            // 跳过不支持的资源类型。
            if (formType == null) {
                continue;
            }
            // 获取资源名称并构建待办项。
            String resourceName = Optional.ofNullable(resourceNameMap.get(formType))
                    .map(map -> map.get(instance.getResourceId()))
                    .orElse(StringUtils.EMPTY);
            ApprovalTodoItemResponse item = new ApprovalTodoItemResponse();
            item.setResourceId(instance.getResourceId());
            item.setResourceName(resourceName);
            item.setResourceType(formType.name());
            item.setApplicant(submitterMap.get(instance.getSubmitterId()));
            item.setSubmitTime(instance.getSubmitTime());
            switch (formType) {
                case QUOTATION -> response.getQuotation().add(item);
                case CONTRACT -> response.getContract().add(item);
                case ORDER -> response.getOrder().add(item);
                case INVOICE -> response.getInvoice().add(item);
            }
        }
        // 返回分类后的待办集合。
        return response;
    }

    private ApprovalTodoResponse initResponse() {
        // 初始化四类待办列表，避免空指针。
        ApprovalTodoResponse response = new ApprovalTodoResponse();
        response.setQuotation(new ArrayList<>());
        response.setContract(new ArrayList<>());
        response.setOrder(new ArrayList<>());
        response.setInvoice(new ArrayList<>());
        return response;
    }

    private Map<String, String> loadSubmitterNameMap(List<ApprovalInstance> instances) {
        // 提取申请人ID并去重。
        List<String> submitterIds = instances.stream()
                .map(ApprovalInstance::getSubmitterId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (submitterIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 批量查询申请人名称映射。
        return userMapper.selectByIds(submitterIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (prev, next) -> prev));
    }

    private Map<ApprovalFormTypeEnum, Map<String, String>> loadResourceNameMap(List<ApprovalInstance> instances) {
        // 先按资源类型分组收集资源ID。
        Map<ApprovalFormTypeEnum, List<String>> typeResourceIds = new EnumMap<>(ApprovalFormTypeEnum.class);
        for (ApprovalInstance instance : instances) {
            ApprovalFormTypeEnum formType = parseFormType(instance.getType());
            if (formType == null || StringUtils.isBlank(instance.getResourceId())) {
                continue;
            }
            typeResourceIds.computeIfAbsent(formType, key -> new ArrayList<>()).add(instance.getResourceId());
        }

        // 按类型批量查询名称并构造统一映射。
        Map<ApprovalFormTypeEnum, Map<String, String>> resourceNameMap = new EnumMap<>(ApprovalFormTypeEnum.class);
        typeResourceIds.forEach((formType, ids) -> {
            List<String> distinctIds = ids.stream().distinct().toList();
            switch (formType) {
                case QUOTATION -> resourceNameMap.put(formType, quotationMapper.selectByIds(distinctIds).stream()
                        .collect(Collectors.toMap(OpportunityQuotation::getId, OpportunityQuotation::getName, (prev, next) -> prev)));
                case CONTRACT -> resourceNameMap.put(formType, contractMapper.selectByIds(distinctIds).stream()
                        .collect(Collectors.toMap(Contract::getId, Contract::getName, (prev, next) -> prev)));
                case ORDER -> resourceNameMap.put(formType, orderMapper.selectByIds(distinctIds).stream()
                        .collect(Collectors.toMap(Order::getId, Order::getName, (prev, next) -> prev)));
                case INVOICE -> resourceNameMap.put(formType, invoiceMapper.selectByIds(distinctIds).stream()
                        .collect(Collectors.toMap(ContractInvoice::getId, ContractInvoice::getName, (prev, next) -> prev)));
            }
        });
        return resourceNameMap;
    }

    private ApprovalFormTypeEnum parseFormType(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        // 优先按枚举名匹配（如 QUOTATION/CONTRACT/ORDER/INVOICE）。
        for (ApprovalFormTypeEnum formType : ApprovalFormTypeEnum.values()) {
            if (StringUtils.equalsIgnoreCase(formType.name(), type)) {
                return formType;
            }
        }
        // 兼容旧值或别名写法。
        return switch (type.toLowerCase()) {
            case "quote" -> ApprovalFormTypeEnum.QUOTATION;
            case "quotation" -> ApprovalFormTypeEnum.QUOTATION;
            case "contract" -> ApprovalFormTypeEnum.CONTRACT;
            case "order" -> ApprovalFormTypeEnum.ORDER;
            case "invoice" -> ApprovalFormTypeEnum.INVOICE;
            default -> null;
        };
    }
}
