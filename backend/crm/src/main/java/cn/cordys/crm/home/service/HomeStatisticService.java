package cn.cordys.crm.home.service;

import cn.cordys.common.constants.BusinessSearchType;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.RoleDataScopeDTO;
import cn.cordys.common.dto.RolePermissionDTO;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.LogUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.home.constants.HomeStatisticPeriod;
import cn.cordys.crm.home.dto.request.HomeStatisticBaseSearchRequest;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchRequest;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import cn.cordys.crm.home.dto.response.HomeClueStatistic;
import cn.cordys.crm.home.dto.response.HomeOpportunityStatistic;
import cn.cordys.crm.home.dto.response.HomeStatisticSearchResponse;
import cn.cordys.crm.home.dto.response.HomeSuccessOpportunityStatistic;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class HomeStatisticService {

    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private RoleService roleService;

    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public HomeClueStatistic getClueStatistic(HomeStatisticBaseSearchRequest request, DeptDataPermissionDTO deptDataPermission, String orgId, String userId) {
        HomeClueStatistic clueStatistic = new HomeClueStatistic();
        try {
            for (HomeStatisticPeriod statisticPeriod : HomeStatisticPeriod.values()) {
                HomeStatisticSearchRequest searchRequest = BeanUtils.copyBean(new HomeStatisticSearchRequest(), request);
                searchRequest.setPeriod(statisticPeriod.name());
                HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(searchRequest, deptDataPermission, orgId, userId);

                // 多线程执行
                Future<HomeStatisticSearchResponse> getNewClueStatistic = executor.submit(() ->
                        getStatisticSearchResponse(wrapperRequest, this::getNewClueCount));

                switch (statisticPeriod) {
                    case TODAY -> clueStatistic.setTodayClue(getNewClueStatistic.get());
                    case THIS_WEEK -> clueStatistic.setThisWeekClue(getNewClueStatistic.get());
                    case THIS_MONTH -> clueStatistic.setThisMonthClue(getNewClueStatistic.get());
                    case THIS_YEAR -> clueStatistic.setThisYearClue(getNewClueStatistic.get());
                }
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
        return clueStatistic;
    }

    public HomeOpportunityStatistic getOpportunityStatistic(HomeStatisticBaseSearchRequest request, DeptDataPermissionDTO deptDataPermission, String orgId, String userId) {
        HomeOpportunityStatistic opportunityStatistic = new HomeOpportunityStatistic();
        for (HomeStatisticPeriod statisticPeriod : HomeStatisticPeriod.values()) {
            HomeStatisticSearchRequest searchRequest = BeanUtils.copyBean(new HomeStatisticSearchRequest(), request);
            searchRequest.setPeriod(statisticPeriod.name());
            HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(searchRequest, deptDataPermission, orgId, userId);

            // 多线程执行
            Future<HomeStatisticSearchResponse> getNewOpportunityStatistic = executor.submit(() ->
                    getStatisticSearchResponse(wrapperRequest, this::getNewOpportunityCount));
            Future<HomeStatisticSearchResponse> getNewOpportunityTotalAmount = executor.submit(() ->
                    getStatisticSearchResponse(wrapperRequest, this::getNewOpportunityAmount));
            try {
                switch (statisticPeriod) {
                    case TODAY -> {
                        opportunityStatistic.setTodayOpportunity(getNewOpportunityStatistic.get());
                        opportunityStatistic.setTodayOpportunityAmount(getNewOpportunityTotalAmount.get());
                    }
                    case THIS_WEEK -> {
                        opportunityStatistic.setThisWeekOpportunity(getNewOpportunityStatistic.get());
                        opportunityStatistic.setThisWeekOpportunityAmount(getNewOpportunityTotalAmount.get());
                    }
                    case THIS_MONTH -> {
                        opportunityStatistic.setThisMonthOpportunity(getNewOpportunityStatistic.get());
                        opportunityStatistic.setThisMonthOpportunityAmount(getNewOpportunityTotalAmount.get());
                    }
                    case THIS_YEAR -> {
                        opportunityStatistic.setThisYearOpportunity(getNewOpportunityStatistic.get());
                        opportunityStatistic.setThisYearOpportunityAmount(getNewOpportunityTotalAmount.get());
                    }
                }
            } catch (Exception e) {
                LogUtils.error(e);
            }
        }
        return opportunityStatistic;
    }

    public HomeSuccessOpportunityStatistic getSuccessOpportunityStatistic(HomeStatisticBaseSearchRequest request, DeptDataPermissionDTO deptDataPermission, String orgId, String userId) {
        HomeSuccessOpportunityStatistic opportunityStatistic = new HomeSuccessOpportunityStatistic();
        for (HomeStatisticPeriod statisticPeriod : HomeStatisticPeriod.values()) {
            HomeStatisticSearchRequest searchRequest = BeanUtils.copyBean(new HomeStatisticSearchRequest(), request);
            searchRequest.setPeriod(statisticPeriod.name());
            HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(searchRequest, deptDataPermission, orgId, userId);

            // 多线程执行
            Future<HomeStatisticSearchResponse> getSuccessOpportunityStatistic = executor.submit(() ->
                    getStatisticSearchResponse(wrapperRequest, this::getSuccessOpportunityCount));
            Future<HomeStatisticSearchResponse> getSuccessOpportunityStatisticAmount = executor.submit(() ->
                    getStatisticSearchResponse(wrapperRequest, this::getSuccessOpportunityAmount));

            try {
                switch (statisticPeriod) {
                    case TODAY -> {
                        opportunityStatistic.setTodayOpportunity(getSuccessOpportunityStatistic.get());
                        opportunityStatistic.setTodayOpportunityAmount(getSuccessOpportunityStatisticAmount.get());
                    }
                    case THIS_WEEK -> {
                        opportunityStatistic.setThisWeekOpportunity(getSuccessOpportunityStatistic.get());
                        opportunityStatistic.setThisWeekOpportunityAmount(getSuccessOpportunityStatisticAmount.get());
                    }
                    case THIS_MONTH -> {
                        opportunityStatistic.setThisMonthOpportunity(getSuccessOpportunityStatistic.get());
                        opportunityStatistic.setThisMonthOpportunityAmount(getSuccessOpportunityStatisticAmount.get());
                    }
                    case THIS_YEAR -> {
                        opportunityStatistic.setThisYearOpportunity(getSuccessOpportunityStatistic.get());
                        opportunityStatistic.setThisYearOpportunityAmount(getSuccessOpportunityStatisticAmount.get());
                    }
                }
            } catch (Exception e) {
                LogUtils.error(e);
            }
        }
        return opportunityStatistic;
    }

    /**
     * 获取新增线索统计
     *
     * @param request
     *
     * @return
     */
    public Long getNewClueCount(HomeStatisticSearchWrapperRequest request) {
        return extClueMapper.selectClueCount(request, false);
    }

    private HomeStatisticSearchWrapperRequest copyHomeStatisticSearchWrapperRequest(HomeStatisticSearchWrapperRequest request) {
        HomeStatisticSearchWrapperRequest totalRequest = new HomeStatisticSearchWrapperRequest(BeanUtils.copyBean(new HomeStatisticSearchRequest(), request.getStaticRequest()),
                request.getDataPermission(), request.getOrgId(), request.getUserId());
        return totalRequest;
    }

    /**
     * 获取新增商机数量
     *
     * @param request
     *
     * @return
     */
    public Long getNewOpportunityCount(HomeStatisticSearchWrapperRequest request) {
        return extOpportunityMapper.selectOpportunityCount(request, false, false);
    }

    /**
     * 获取新增商机总额数量
     *
     * @param request
     *
     * @return
     */
    public Long getNewOpportunityAmount(HomeStatisticSearchWrapperRequest request) {
        return Optional.ofNullable(extOpportunityMapper.selectOpportunityCount(request, true, false)).orElse(0L);
    }

    /**
     * 获取赢单数量
     *
     * @param request
     *
     * @return
     */
    public Long getSuccessOpportunityCount(HomeStatisticSearchWrapperRequest request) {
        return extOpportunityMapper.selectOpportunityCount(request, false, true);
    }

    /**
     * 获取赢单总额
     *
     * @param request
     *
     * @return
     */
    public Long getSuccessOpportunityAmount(HomeStatisticSearchWrapperRequest request) {
        return Optional.ofNullable(extOpportunityMapper.selectOpportunityCount(request, true, true)).orElse(0L);
    }

    /**
     * 获取统计数量和较上期对比率的通用方法
     *
     * @param request
     * @param statisticFunction
     *
     * @return
     */
    public HomeStatisticSearchResponse getStatisticSearchResponse(HomeStatisticSearchWrapperRequest request,
                                                                  Function<HomeStatisticSearchWrapperRequest, Long> statisticFunction) {
        HomeStatisticSearchResponse response = new HomeStatisticSearchResponse();
        Future<Long> getCount = executor.submit(() -> statisticFunction.apply(request));
        try {

            Long count = getCount.get();
            response.setValue(count);

            if (request.comparePeriod()) {
                HomeStatisticSearchWrapperRequest periodRequest = copyHomeStatisticSearchWrapperRequest(request);
                periodRequest.setStartTime(periodRequest.getPeriodStartTime());
                periodRequest.setEndTime(periodRequest.getPeriodEndTime());
                Future<Long> getPeriodCount = executor.submit(() -> statisticFunction.apply(periodRequest));
                Long periodCount = getPeriodCount.get();
                response.setPriorPeriodCompareRate(getPriorPeriodCompareRate(count, periodCount));
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
        return response;
    }

    private Double getPriorPeriodCompareRate(Long count, Long periodCount) {
        return periodCount == null || periodCount == 0 ? null : (count - periodCount) * 100.0 / periodCount;
    }

    /**
     * 获取部门数据权限
     *
     * @param request
     * @param permission
     *
     * @return
     */
    public DeptDataPermissionDTO getDeptDataPermissionDTO(HomeStatisticBaseSearchRequest request, String permission) {
        if (Strings.CS.equals(request.getSearchType(), BusinessSearchType.DEPARTMENT.name())) {
            DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                    OrganizationContext.getOrganizationId(), permission);
            if (deptDataPermission.getAll()) {
                // 如果是全部权限，则不需要过滤部门ID
                deptDataPermission.setDeptIds(request.getDeptIds());
            } else if (CollectionUtils.isNotEmpty(deptDataPermission.getDeptIds())) {
                // 如果只有部门权限，则过滤掉没有权限的部门ID
                Set<String> deptIds = request.getDeptIds()
                        .stream()
                        .filter(deptId -> CollectionUtils.isNotEmpty(deptDataPermission.getDeptIds())
                                && deptDataPermission.getDeptIds().contains(deptId))
                        .collect(Collectors.toSet());
                deptDataPermission.setDeptIds(deptIds);
            }
            return deptDataPermission;
        } else if (Strings.CS.equals(request.getSearchType(), BusinessSearchType.ALL.name())) {
            DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                    OrganizationContext.getOrganizationId(), permission);
            return deptDataPermission;
        } else {
            DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();
            deptDataPermission.setSelf(true);
            return deptDataPermission;
        }
    }

    public List<BaseTreeNode> getDepartmentTree(String userId, String orgId) {
        Map<String, List<RolePermissionDTO>> dataScopeRoleMap = permissionCache.getRolePermissions(userId, orgId)
                .stream()
                .collect(Collectors.groupingBy(RolePermissionDTO::getDataScope, Collectors.toList()));

        boolean hasAllPermission = dataScopeRoleMap.get(RoleDataScope.ALL.name()) != null;
        boolean hasDeptAndChildPermission = dataScopeRoleMap.get(RoleDataScope.DEPT_AND_CHILD.name()) != null;
        boolean hasDeptCustomPermission = dataScopeRoleMap.get(RoleDataScope.DEPT_CUSTOM.name()) != null;

        if (hasAllPermission || Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
            // 如果有全部数据权限，则返回所有部门树
            return departmentService.getTree(orgId);
        }

        if (hasDeptAndChildPermission || hasDeptCustomPermission) {
            List<BaseTreeNode> tree = departmentService.getTree(orgId);
            Set<String> deptIds = new HashSet<>();
            if (hasDeptAndChildPermission) {
                // 查询本部门数据
                OrganizationUser organizationUser = dataScopeService.getOrganizationUser(userId, orgId);
                deptIds.addAll(List.of(organizationUser.getDepartmentId()));
            }

            if (hasDeptCustomPermission) {
                // 查看指定部门及其子部门数据
                List<String> customDeptRolesIds = dataScopeRoleMap.get(RoleDataScope.DEPT_CUSTOM.name()).stream()
                        .map(RoleDataScopeDTO::getId)
                        .toList();
                List<String> parentDeptIds = roleService.getDeptIdsByRoleIds(customDeptRolesIds);
                deptIds.addAll(parentDeptIds);
            }
            // 查询子部门数据
            deptIds = new HashSet<>(dataScopeService.getDeptIdsWithChild(tree, deptIds));

            return pruningTree(tree, deptIds);
        }

        return List.of();
    }

    /**
     * 剪枝
     * 从树中移除不在已选部门ID中的节点，同时保留已选部门ID的子节点
     *
     * @param tree
     * @param deptIds
     *
     * @return
     */
    public List<BaseTreeNode> pruningTree(List<BaseTreeNode> tree, Set<String> deptIds) {
        Iterator<BaseTreeNode> iterator = tree.iterator();

        List<BaseTreeNode> addNodes = new ArrayList<>();
        while (iterator.hasNext()) {
            BaseTreeNode node = iterator.next();
            List<BaseTreeNode> children = node.getChildren();
            if (CollectionUtils.isNotEmpty(node.getChildren())) {
                // 递归搜索子节点
                node.setChildren(pruningTree(children, deptIds));
            }
            if (!deptIds.contains(node.getId())) {
                // 如果当前节点不在已选部门ID中，并且没有子部门，则移除该节点
                iterator.remove();
                if (CollectionUtils.isNotEmpty(node.getChildren())) {
                    // 如果当前节点有子部门，则保留子部门
                    for (BaseTreeNode child : node.getChildren()) {
                        addNodes.add(child);
                    }
                }
            }
        }
        tree.addAll(addNodes);
        return tree;
    }

    public boolean isEmptyDeptData(String searchType, DeptDataPermissionDTO deptDataPermission) {
        return Strings.CS.equals(searchType, BusinessSearchType.DEPARTMENT.name())
                && CollectionUtils.isEmpty(deptDataPermission.getDeptIds());
    }
}