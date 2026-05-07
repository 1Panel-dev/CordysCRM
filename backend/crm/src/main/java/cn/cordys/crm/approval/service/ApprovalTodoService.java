package cn.cordys.crm.approval.service;

import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.approval.dto.request.ApprovalProcessedPageRequest;
import cn.cordys.crm.approval.dto.request.ApprovalTodoPageRequest;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalState;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractInvoice;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.system.domain.User;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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

    public Pager<List<ApprovalTodoItemResponse>> getTodoPage(ApprovalTodoPageRequest request, String userId) {
        // 在未登录场景下直接返回空分页数据。
        if (StringUtils.isBlank(userId)) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 解析资源类型过滤参数，支持 ALL 或具体类型。
        ApprovalFormTypeEnum filterType = parseFilterType(request.getResourceType());
        if (!isAllType(request.getResourceType()) && filterType == null) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 在指定类型场景下先预查实例ID，缩小任务查询范围。
        List<String> scopedInstanceIds = Collections.emptyList();
        if (filterType != null) {
            scopedInstanceIds = loadInstanceIdsByType(filterType);
            if (scopedInstanceIds.isEmpty()) {
                return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
            }
        }

        // 分页查询当前用户待审批任务，并按更新时间倒序返回。
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getApproverId, userId)
                .eq(ApprovalTask::getTaskStatus, ApprovalState.PENDING.getId())
                .orderByDesc(ApprovalTask::getUpdateTime);
        if (!scopedInstanceIds.isEmpty()) {
            taskWrapper.in(ApprovalTask::getInstanceId, scopedInstanceIds);
        }
        List<ApprovalTask> tasks = approvalTaskMapper.selectListByLambda(taskWrapper);
        if (tasks.isEmpty()) {
            return PageUtils.setPageInfo(page, Collections.<ApprovalTodoItemResponse>emptyList());
        }

        // 收集任务对应审批实例ID。
        List<String> instanceIds = tasks.stream()
                .map(ApprovalTask::getInstanceId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (instanceIds.isEmpty()) {
            return PageUtils.setPageInfo(page, Collections.<ApprovalTodoItemResponse>emptyList());
        }

        // 批量查询审批实例并建立映射。
        Map<String, ApprovalInstance> instanceMap = approvalInstanceMapper.selectByIds(instanceIds).stream()
                .collect(Collectors.toMap(ApprovalInstance::getId, Function.identity(), (prev, next) -> prev));
        if (instanceMap.isEmpty()) {
            return PageUtils.setPageInfo(page, Collections.<ApprovalTodoItemResponse>emptyList());
        }

        // 仅保留当前节点上的待审批任务，避免混入历史节点任务。
        List<ApprovalTask> currentNodeTasks = tasks.stream()
                .filter(task -> {
                    ApprovalInstance instance = instanceMap.get(task.getInstanceId());
                    return instance != null && Strings.CS.equals(instance.getCurrentNodeId(), task.getNodeId());
                })
                .toList();
        if (currentNodeTasks.isEmpty()) {
            return PageUtils.setPageInfo(page, Collections.<ApprovalTodoItemResponse>emptyList());
        }

        // 预加载申请人名称和资源名称映射，减少循环内查询。
        List<ApprovalInstance> instances = currentNodeTasks.stream()
                .map(task -> instanceMap.get(task.getInstanceId()))
                .filter(Objects::nonNull)
                .toList();
        Map<String, String> submitterMap = loadSubmitterNameMap(instances);
        Map<ApprovalFormTypeEnum, Map<String, String>> resourceNameMap = loadResourceNameMap(instances);

        // 逐条组装待办分页数据并去重实例。
        List<ApprovalTodoItemResponse> list = new ArrayList<>(currentNodeTasks.size());
        Set<String> processedInstanceIds = new HashSet<>();
        for (ApprovalTask task : currentNodeTasks) {
            if (!processedInstanceIds.add(task.getInstanceId())) {
                continue;
            }
            ApprovalInstance instance = instanceMap.get(task.getInstanceId());
            if (instance == null) {
                continue;
            }
            ApprovalFormTypeEnum formType = parseFormType(instance.getType());
            if (formType == null) {
                continue;
            }
            if (filterType != null && filterType != formType) {
                continue;
            }
            String resourceName = Optional.ofNullable(resourceNameMap.get(formType))
                    .map(map -> map.get(instance.getResourceId()))
                    .orElse(StringUtils.EMPTY);
            ApprovalTodoItemResponse item = new ApprovalTodoItemResponse();
            item.setResourceId(instance.getResourceId());
            item.setResourceName(resourceName);
            item.setResourceType(formType.name());
            item.setApplicant(submitterMap.get(instance.getSubmitterId()));
            item.setSubmitTime(instance.getSubmitTime());
            item.setApprovalOperation(task.getTaskStatus());
            item.setDataResult(instance.getResult());
            list.add(item);
        }
        // 返回分页待办列表。
        return PageUtils.setPageInfo(page, list);
    }

    public Pager<List<ApprovalTodoItemResponse>> getProcessedPage(ApprovalProcessedPageRequest request, String userId) {
        // 在未登录场景下直接返回空分页数据。
        if (StringUtils.isBlank(userId)) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 分页查询当前用户已处理任务，并按更新时间倒序返回最新处理记录。
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getApproverId, userId)
                .nq(ApprovalTask::getTaskStatus, ApprovalState.PENDING.getId())
                .orderByDesc(ApprovalTask::getUpdateTime);
        List<ApprovalTask> tasks = approvalTaskMapper.selectListByLambda(taskWrapper);
        if (tasks.isEmpty()) {
            return PageUtils.setPageInfo(page, Collections.<ApprovalTodoItemResponse>emptyList());
        }

        // 批量加载任务对应审批实例，避免循环查询实例数据。
        List<String> instanceIds = tasks.stream()
                .map(ApprovalTask::getInstanceId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        Map<String, ApprovalInstance> instanceMap = approvalInstanceMapper.selectByIds(instanceIds).stream()
                .collect(Collectors.toMap(ApprovalInstance::getId, Function.identity(), (prev, next) -> prev));
        if (instanceMap.isEmpty()) {
            return PageUtils.setPageInfo(page, Collections.<ApprovalTodoItemResponse>emptyList());
        }

        // 预加载申请人名称和资源名称映射，减少组装阶段的重复计算。
        List<ApprovalInstance> instances = tasks.stream()
                .map(task -> instanceMap.get(task.getInstanceId()))
                .filter(Objects::nonNull)
                .toList();
        Map<String, String> submitterMap = loadSubmitterNameMap(instances);
        Map<ApprovalFormTypeEnum, Map<String, String>> resourceNameMap = loadResourceNameMap(instances);

        // 逐条组装已处理审批数据并回填审批操作和数据结果字段。
        List<ApprovalTodoItemResponse> list = new ArrayList<>(tasks.size());
        for (ApprovalTask task : tasks) {
            ApprovalInstance instance = instanceMap.get(task.getInstanceId());
            if (instance == null) {
                continue;
            }
            ApprovalFormTypeEnum formType = parseFormType(instance.getType());
            if (formType == null) {
                continue;
            }
            String resourceName = Optional.ofNullable(resourceNameMap.get(formType))
                    .map(map -> map.get(instance.getResourceId()))
                    .orElse(StringUtils.EMPTY);
            ApprovalTodoItemResponse item = new ApprovalTodoItemResponse();
            item.setResourceId(instance.getResourceId());
            item.setResourceName(resourceName);
            item.setResourceType(formType.name());
            item.setApplicant(submitterMap.get(instance.getSubmitterId()));
            item.setSubmitTime(instance.getSubmitTime());
            item.setApprovalOperation(task.getTaskStatus());
            item.setDataResult(instance.getResult());
            list.add(item);
        }

        // 返回分页结果，分页元信息沿用 PageHelper 查询结果。
        return PageUtils.setPageInfo(page, list);
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
            if (Strings.CI.equals(formType.name(), type)) {
                return formType;
            }
        }
        // 兼容旧值或别名写法。
        return switch (type.toLowerCase()) {
            case "quote", "quotation" -> ApprovalFormTypeEnum.QUOTATION;
            case "contract" -> ApprovalFormTypeEnum.CONTRACT;
            case "order" -> ApprovalFormTypeEnum.ORDER;
            case "invoice" -> ApprovalFormTypeEnum.INVOICE;
            default -> null;
        };
    }

    private boolean isAllType(String resourceType) {
        return StringUtils.isBlank(resourceType) || StringUtils.equalsIgnoreCase(resourceType, "ALL");
    }

    private ApprovalFormTypeEnum parseFilterType(String resourceType) {
        if (isAllType(resourceType)) {
            return null;
        }
        return parseFormType(resourceType);
    }

    private List<String> loadInstanceIdsByType(ApprovalFormTypeEnum formType) {
        List<String> aliases = switch (formType) {
            case QUOTATION -> List.of("quotation", "quote");
            case CONTRACT -> List.of("contract");
            case ORDER -> List.of("order");
            case INVOICE -> List.of("invoice");
        };
        LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ApprovalInstance::getType, aliases);
        return approvalInstanceMapper.selectListByLambda(wrapper).stream()
                .map(ApprovalInstance::getId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }
}
