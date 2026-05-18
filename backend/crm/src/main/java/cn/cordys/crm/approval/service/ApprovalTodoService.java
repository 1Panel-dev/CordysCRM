package cn.cordys.crm.approval.service;

import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalState;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalProcessedPageRequest;
import cn.cordys.crm.approval.dto.request.ApprovalTodoPageRequest;
import cn.cordys.crm.approval.dto.response.ApprovalTodoCountResponse;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
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
    @Resource
    private ExtApprovalTaskMapper extApprovalTaskMapper;

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

        // 先根据资源名称预查实例ID，保证分页总数与筛选条件一致。
        Set<String> nameFilteredInstanceIds = loadInstanceIdsByResourceName(request.getResourceName());
        if (nameFilteredInstanceIds != null && nameFilteredInstanceIds.isEmpty()) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 在指定类型场景下先预查实例ID，并与名称筛选结果合并。
        List<String> scopedInstanceIds = Collections.emptyList();
        if (filterType != null) {
            scopedInstanceIds = loadInstanceIdsByType(filterType);
            if (scopedInstanceIds.isEmpty()) {
                return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
            }
        }
        List<String> finalScopedInstanceIds = mergeInstanceIdScope(scopedInstanceIds, nameFilteredInstanceIds);
        if (nameFilteredInstanceIds != null && finalScopedInstanceIds.isEmpty()) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 分页查询当前用户待审批任务，并按更新时间倒序返回。
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getApproverId, userId)
                .eq(ApprovalTask::getStatus, ApprovalState.APPROVING.getId())
                .orderByDesc(ApprovalTask::getUpdateTime);
        if (!finalScopedInstanceIds.isEmpty()) {
            taskWrapper.in(ApprovalTask::getInstanceId, finalScopedInstanceIds);
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
            item.setApprovalOperation(StringUtils.defaultIfBlank(task.getAction(), task.getStatus()));
            item.setDataResult(instance.getApprovalStatus());
            item.setApprovalTaskId(task.getId());
            item.setApprovalNodeId(task.getNodeId());
            item.setApprovalInstanceId(task.getInstanceId());
            item.setApprovalId(task.getApproverId());
            list.add(item);
        }
        // 返回分页待办列表。
        return PageUtils.setPageInfo(page, list);
    }

    public ApprovalTodoCountResponse getPendingCount(String userId) {
        // 初始化统计响应对象并设置默认值。
        ApprovalTodoCountResponse response = new ApprovalTodoCountResponse();
        response.setTotal(0);
        response.setQuotation(0);
        response.setContract(0);
        response.setOrder(0);
        response.setInvoice(0);
        // 未登录用户直接返回空统计。
        if (StringUtils.isBlank(userId)) {
            return response;
        }
        // 通过聚合SQL统计待我审批总数及资源类型分布。
        ApprovalTodoCountResponse count = extApprovalTaskMapper.countPendingByApprover(userId, ApprovalState.APPROVING.getId());
        if (count == null) {
            return response;
        }
        response.setTotal(Optional.ofNullable(count.getTotal()).orElse(0));
        response.setQuotation(Optional.ofNullable(count.getQuotation()).orElse(0));
        response.setContract(Optional.ofNullable(count.getContract()).orElse(0));
        response.setOrder(Optional.ofNullable(count.getOrder()).orElse(0));
        response.setInvoice(Optional.ofNullable(count.getInvoice()).orElse(0));
        return response;
    }

    public Pager<List<ApprovalTodoItemResponse>> getProcessedPage(ApprovalProcessedPageRequest request, String userId) {
        // 在未登录场景下直接返回空分页数据。
        if (StringUtils.isBlank(userId)) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 先根据资源名称预查实例ID，保证分页总数与筛选条件一致。
        Set<String> nameFilteredInstanceIds = loadInstanceIdsByResourceName(request.getResourceName());
        if (nameFilteredInstanceIds != null && nameFilteredInstanceIds.isEmpty()) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 分页查询当前用户已处理任务，并按更新时间倒序返回最新处理记录。
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getApproverId, userId)
                .nq(ApprovalTask::getStatus, ApprovalState.PENDING.getId())
                .orderByDesc(ApprovalTask::getUpdateTime);
        if (nameFilteredInstanceIds != null) {
            taskWrapper.in(ApprovalTask::getInstanceId, new ArrayList<>(nameFilteredInstanceIds));
        }
        List<ApprovalTask> tasks = approvalTaskMapper.selectListByLambda(taskWrapper);
        return buildTaskPageResult(page, tasks);
    }

    public Pager<List<ApprovalTodoItemResponse>> getInitiatedPage(ApprovalProcessedPageRequest request, String userId) {
        // 在未登录场景下直接返回空分页数据。
        if (StringUtils.isBlank(userId)) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 先根据资源名称预查实例ID，保证分页总数与筛选条件一致。
        Set<String> nameFilteredInstanceIds = loadInstanceIdsByResourceName(request.getResourceName());
        if (nameFilteredInstanceIds != null && nameFilteredInstanceIds.isEmpty()) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 分页查询当前用户发起的审批实例，并按更新时间倒序返回。
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalInstance::getSubmitterId, userId)
                .orderByDesc(ApprovalInstance::getUpdateTime);
        if (nameFilteredInstanceIds != null) {
            wrapper.in(ApprovalInstance::getId, new ArrayList<>(nameFilteredInstanceIds));
        }
        List<ApprovalInstance> instances = approvalInstanceMapper.selectListByLambda(wrapper);
        if (instances.isEmpty()) {
            return PageUtils.setPageInfo(page, Collections.<ApprovalTodoItemResponse>emptyList());
        }

        // 预加载申请人名称和资源名称映射，减少组装阶段重复查询。
        Map<String, String> submitterMap = loadSubmitterNameMap(instances);
        Map<ApprovalFormTypeEnum, Map<String, String>> resourceNameMap = loadResourceNameMap(instances);

        // 逐条组装我发起的审批分页结果。
        List<ApprovalTodoItemResponse> list = new ArrayList<>(instances.size());
        for (ApprovalInstance instance : instances) {
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
            item.setApprovalOperation(instance.getApprovalStatus());
            item.setDataResult(instance.getApprovalStatus());
            list.add(item);
        }
        // 返回分页结果，分页元信息沿用 PageHelper 查询结果。
        return PageUtils.setPageInfo(page, list);
    }

    public Pager<List<ApprovalTodoItemResponse>> getCcPage(ApprovalProcessedPageRequest request, String userId) {
        // 在未登录场景下直接返回空分页数据。
        if (StringUtils.isBlank(userId)) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 先根据资源名称预查实例ID，保证分页总数与筛选条件一致。
        Set<String> nameFilteredInstanceIds = loadInstanceIdsByResourceName(request.getResourceName());
        if (nameFilteredInstanceIds != null && nameFilteredInstanceIds.isEmpty()) {
            return new Pager<>(Collections.<ApprovalTodoItemResponse>emptyList(), 0, request.getPageSize(), request.getCurrent());
        }

        // 分页查询抄送给当前用户的任务记录，并按更新时间倒序返回。
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getApproverId, userId)
                .orderByDesc(ApprovalTask::getUpdateTime);
        taskWrapper.in(ApprovalTask::getType, List.of("cc", "CC"));
        if (nameFilteredInstanceIds != null) {
            taskWrapper.in(ApprovalTask::getInstanceId, new ArrayList<>(nameFilteredInstanceIds));
        }
        List<ApprovalTask> tasks = approvalTaskMapper.selectListByLambda(taskWrapper);
        return buildTaskPageResult(page, tasks);
    }

    public void deleteApprovalTaskByInstanceId(String approvalInstanceId) {
        // 实例ID为空时不执行删除。
        if (StringUtils.isBlank(approvalInstanceId)) {
            return;
        }
        // 按审批实例ID删除审批任务。
        approvalTaskMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, approvalInstanceId));
    }

    public void deleteApprovalTaskByInstanceIds(List<String> approvalInstanceIds) {
        // 实例ID集合为空时不执行删除。
        if (approvalInstanceIds == null || approvalInstanceIds.isEmpty()) {
            return;
        }
        // 过滤空白实例ID并批量删除审批任务。
        List<String> validInstanceIds = approvalInstanceIds.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (validInstanceIds.isEmpty()) {
            return;
        }
        approvalTaskMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalTask>()
                .in(ApprovalTask::getInstanceId, validInstanceIds));
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

    private List<String> mergeInstanceIdScope(List<String> typeInstanceIds, Set<String> nameInstanceIds) {
        if (nameInstanceIds == null) {
            return typeInstanceIds;
        }
        if (typeInstanceIds.isEmpty()) {
            return new ArrayList<>(nameInstanceIds);
        }
        Set<String> intersection = new HashSet<>(typeInstanceIds);
        intersection.retainAll(nameInstanceIds);
        return new ArrayList<>(intersection);
    }

    private Set<String> loadInstanceIdsByResourceName(String resourceName) {
        if (StringUtils.isBlank(resourceName)) {
            return null;
        }
        // 按资源名称模糊匹配资源表，构造资源类型到资源ID的映射。
        Map<ApprovalFormTypeEnum, List<String>> resourceIdsByType = new EnumMap<>(ApprovalFormTypeEnum.class);
        LambdaQueryWrapper<OpportunityQuotation> quotationWrapper = new LambdaQueryWrapper<>();
        quotationWrapper.like(OpportunityQuotation::getName, resourceName);
        List<String> quotationIds = quotationMapper.selectListByLambda(quotationWrapper).stream()
                .map(OpportunityQuotation::getId).filter(StringUtils::isNotBlank).toList();
        if (!quotationIds.isEmpty()) {
            resourceIdsByType.put(ApprovalFormTypeEnum.QUOTATION, quotationIds);
        }
        LambdaQueryWrapper<Contract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.like(Contract::getName, resourceName);
        List<String> contractIds = contractMapper.selectListByLambda(contractWrapper).stream()
                .map(Contract::getId).filter(StringUtils::isNotBlank).toList();
        if (!contractIds.isEmpty()) {
            resourceIdsByType.put(ApprovalFormTypeEnum.CONTRACT, contractIds);
        }
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.like(Order::getName, resourceName);
        List<String> orderIds = orderMapper.selectListByLambda(orderWrapper).stream()
                .map(Order::getId).filter(StringUtils::isNotBlank).toList();
        if (!orderIds.isEmpty()) {
            resourceIdsByType.put(ApprovalFormTypeEnum.ORDER, orderIds);
        }
        LambdaQueryWrapper<ContractInvoice> invoiceWrapper = new LambdaQueryWrapper<>();
        invoiceWrapper.like(ContractInvoice::getName, resourceName);
        List<String> invoiceIds = invoiceMapper.selectListByLambda(invoiceWrapper).stream()
                .map(ContractInvoice::getId).filter(StringUtils::isNotBlank).toList();
        if (!invoiceIds.isEmpty()) {
            resourceIdsByType.put(ApprovalFormTypeEnum.INVOICE, invoiceIds);
        }
        if (resourceIdsByType.isEmpty()) {
            return Collections.emptySet();
        }

        // 按资源类型与资源ID反查审批实例ID。
        Set<String> instanceIds = new HashSet<>();
        resourceIdsByType.forEach((formType, resourceIds) -> {
            List<String> aliases = switch (formType) {
                case QUOTATION -> List.of("quotation", "quote");
                case CONTRACT -> List.of("contract");
                case ORDER -> List.of("order");
                case INVOICE -> List.of("invoice");
            };
            LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ApprovalInstance::getType, aliases)
                    .in(ApprovalInstance::getResourceId, resourceIds);
            instanceIds.addAll(approvalInstanceMapper.selectListByLambda(wrapper).stream()
                    .map(ApprovalInstance::getId)
                    .filter(StringUtils::isNotBlank)
                    .toList());
        });
        return instanceIds;
    }

    private Pager<List<ApprovalTodoItemResponse>> buildTaskPageResult(Page<Object> page, List<ApprovalTask> tasks) {
        // 无任务时直接返回空分页数据。
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

        // 逐条组装审批任务分页结果并回填审批操作和数据结果字段。
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
            item.setApprovalOperation(StringUtils.defaultIfBlank(task.getAction(), task.getStatus()));
            item.setDataResult(instance.getApprovalStatus());
            item.setApprovalTaskId(task.getId());
            item.setApprovalNodeId(task.getNodeId());
            item.setApprovalInstanceId(task.getInstanceId());
            item.setApprovalId(task.getApproverId());
            list.add(item);
        }
        // 返回分页结果，分页元信息沿用 PageHelper 查询结果。
        return PageUtils.setPageInfo(page, list);
    }

}
