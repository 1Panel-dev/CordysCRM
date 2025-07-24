package io.cordys.crm.home.service;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.constants.RoleDataScope;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.RoleDataScopeDTO;
import io.cordys.common.dto.RolePermissionDTO;
import io.cordys.common.permission.PermissionCache;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.LogUtils;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.clue.service.PoolClueService;
import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.customer.service.PoolCustomerService;
import io.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import io.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import io.cordys.crm.home.dto.request.HomeStatisticSearchRequest;
import io.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import io.cordys.crm.home.dto.response.*;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.service.DepartmentService;
import io.cordys.crm.system.service.RoleService;
import io.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
	private ExtCustomerMapper extCustomerMapper;
	@Resource
	private ExtClueMapper extClueMapper;
	@Resource
	private ExtOpportunityMapper extOpportunityMapper;
	@Resource
	private ExtCustomerContactMapper extCustomerContactMapper;
	@Resource
	private ExtFollowUpRecordMapper extFollowUpRecordMapper;
	@Resource
	private ExtFollowUpPlanMapper extFollowUpPlanMapper;
	@Resource
	private DataScopeService dataScopeService;
	@Resource
	private PermissionCache permissionCache;
	@Resource
	private DepartmentService departmentService;
	@Resource
	private RoleService roleService;
	@Resource
	private PoolCustomerService poolCustomerService;
	@Resource
	private PoolClueService poolClueService;

	private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();


	public HomeCustomerStatistic getCustomerStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeCustomerStatistic customerStatistic = new HomeCustomerStatistic();
		try {
			// 多线程执行
			Future<HomeStatisticSearchResponse> getNewCustomerStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getNewCustomerCount));
			Future<HomeStatisticSearchResponse> getUnfollowedCustomerStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getUnfollowedCustomerCount));
			Future<Long> getTotalCustomerCount = executor.submit(() -> getTotalCustomerCount(request));
			Future<Long> getCustomerCapacityCount = executor.submit(() -> getTotalCustomerCapacityCount(request));

			customerStatistic.setNewCustomer(getNewCustomerStatistic.get());
			customerStatistic.setUnfollowedCustomer(getUnfollowedCustomerStatistic.get());
			customerStatistic.setTotal(getTotalCustomerCount.get());

			Long cap = getCustomerCapacityCount.get();
			customerStatistic.setUnConfigured(cap == null);
			if (!customerStatistic.getUnConfigured()) {
				customerStatistic.setRemainingCapacity(cap);
			}
		} catch (Exception e) {
			LogUtils.error(e);
		}
		return customerStatistic;
	}

	public HomeClueStatistic getClueStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeClueStatistic clueStatistic = new HomeClueStatistic();
		try {
			// 多线程执行
			Future<HomeStatisticSearchResponse> getNewClueStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getNewClueCount));
			Future<HomeStatisticSearchResponse> getUnfollowedClueStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getUnfollowedClueCount));
			Future<Long> getTotalClueCount = executor.submit(() -> getTotalClueCount(request));
			Future<Long> getClueCapacityCount = executor.submit(() -> getTotalClueCapacityCount(request));

			clueStatistic.setNewClue(getNewClueStatistic.get());
			clueStatistic.setUnfollowedClue(getUnfollowedClueStatistic.get());
			clueStatistic.setTotal(getTotalClueCount.get());
			Long cap = getClueCapacityCount.get();
			clueStatistic.setUnConfigured(cap == null);
			if (!clueStatistic.getUnConfigured()) {
				clueStatistic.setRemainingCapacity(cap);
			}
			clueStatistic.setRemainingCapacity(getClueCapacityCount.get());
		} catch (Exception e) {
			LogUtils.error(e);
		}
		return clueStatistic;
	}


	public HomeOpportunityStatistic getOpportunityStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeOpportunityStatistic opportunityStatistic = new HomeOpportunityStatistic();
		try {
			// 多线程执行
			Future<HomeStatisticSearchResponse> getNewOpportunityStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getNewOpportunityCount));
			Future<Long> getOpportunityTotalAmount = executor.submit(() -> getOpportunityTotalAmount(request));
			Future<Long> getTotalOpportunityCount = executor.submit(() -> getTotalOpportunityCount(request));

			opportunityStatistic.setNewOpportunity(getNewOpportunityStatistic.get());
			opportunityStatistic.setTotalAmount(getOpportunityTotalAmount.get());
			opportunityStatistic.setTotal(getTotalOpportunityCount.get());
		} catch (Exception e) {
			LogUtils.error(e);
		}
		return opportunityStatistic;
	}

	public HomeContactStatistic getContactStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeContactStatistic contactStatistic = new HomeContactStatistic();
		HomeStatisticSearchResponse statisticSearchResponse =
				getStatisticSearchResponse(request, extCustomerContactMapper::getNewContactCount);
		contactStatistic.setNewContact(statisticSearchResponse);
		return contactStatistic;
	}

	public HomeFollowUpRecordStatistic getFollowUpRecordStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeFollowUpRecordStatistic followUpRecordStatistic = new HomeFollowUpRecordStatistic();
		HomeStatisticSearchResponse statisticSearchResponse =
				getStatisticSearchResponse(request, extFollowUpRecordMapper::getNewContactCount);
		followUpRecordStatistic.setNewFollowUpRecord(statisticSearchResponse);
		return followUpRecordStatistic;
	}

	public HomeFollowUpPlanStatistic getFollowUpPlanStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeFollowUpPlanStatistic followUpPlanStatistic = new HomeFollowUpPlanStatistic();
		HomeStatisticSearchResponse statisticSearchResponse =
				getStatisticSearchResponse(request, extFollowUpPlanMapper::getNewFollowUpPlan);
		followUpPlanStatistic.setNewFollowUpPlan(statisticSearchResponse);
		return followUpPlanStatistic;
	}

	/**
	 * 获取客户总数
	 * @param request
	 * @return
	 */
	public Long getTotalCustomerCount(HomeStatisticSearchWrapperRequest request) {
		HomeStatisticSearchWrapperRequest totalRequest = copyHomeStatisticSearchWrapperRequest(request);
		totalRequest.clearStartTimeAndEndTie();
		return extCustomerMapper.selectCustomerCount(totalRequest, false);
	}

	/**
	 * 获取客户剩余库容
	 * @param request 请求参数
	 * @return 剩余库容数量
	 */
	public Long getTotalCustomerCapacityCount(HomeStatisticSearchWrapperRequest request) {
		HomeStatisticSearchWrapperRequest totalRequest = copyHomeStatisticSearchWrapperRequest(request);
		if (request.getStaticRequest() != null && StringUtils.equals(BusinessSearchType.SELF.name(), totalRequest.getStaticRequest().getSearchType())) {
			return poolCustomerService.getRemainCapacity(request.getUserId(), request.getOrgId());
		}
		return null;
	}

	/**
	 * 获取新增客户统计
	 * @param request
	 * @return
	 */
	public Long getNewCustomerCount(HomeStatisticSearchWrapperRequest request) {
		return extCustomerMapper.selectCustomerCount(request, false);
	}

	/**
	 * 获取未跟进客户统计
	 * @param request
	 * @return
	 */
	public Long getUnfollowedCustomerCount(HomeStatisticSearchWrapperRequest request) {
		return extCustomerMapper.selectCustomerCount(request, true);
	}

	/**
	 * 获取线索总数
	 * @param request
	 * @return
	 */
	public Long getTotalClueCount(HomeStatisticSearchWrapperRequest request) {
		HomeStatisticSearchWrapperRequest totalRequest = copyHomeStatisticSearchWrapperRequest(request);
		totalRequest.clearStartTimeAndEndTie();
		return extClueMapper.selectClueCount(totalRequest, false);
	}

	/**
	 * 获取剩余库容总数
	 * @param request 请求参数
	 * @return 剩余库容数量
	 */
	public Long getTotalClueCapacityCount(HomeStatisticSearchWrapperRequest request) {
		if (request.getStaticRequest() != null && StringUtils.equals(BusinessSearchType.SELF.name(), request.getStaticRequest().getSearchType())) {
			return poolClueService.getRemainCapacity(request.getUserId(), request.getOrgId());
		}
		return null;
	}

	/**
	 * 获取新增线索统计
	 * @param request
	 * @return
	 */
	public Long getNewClueCount(HomeStatisticSearchWrapperRequest request) {
		return extClueMapper.selectClueCount(request, false);
	}

	/**
	 * 获取未跟进线索统计
	 * @param request
	 * @return
	 */
	public Long getUnfollowedClueCount(HomeStatisticSearchWrapperRequest request) {
		return extClueMapper.selectClueCount(request, true);
	}


	/**
	 * 获取客户总数
	 * @param request
	 * @return
	 */
	public Long getTotalOpportunityCount(HomeStatisticSearchWrapperRequest request) {
		HomeStatisticSearchWrapperRequest totalRequest = copyHomeStatisticSearchWrapperRequest(request);
		totalRequest.clearStartTimeAndEndTie();
		return extOpportunityMapper.selectOpportunityCount(totalRequest, false);
	}

	private HomeStatisticSearchWrapperRequest copyHomeStatisticSearchWrapperRequest(HomeStatisticSearchWrapperRequest request) {
		HomeStatisticSearchWrapperRequest totalRequest = new HomeStatisticSearchWrapperRequest(BeanUtils.copyBean(new HomeStatisticSearchRequest(), request.getStaticRequest()),
				request.getDataPermission(), request.getOrgId(), request.getUserId());
		return totalRequest;
	}

	/**
	 * 获取新增客户统计
	 * @param request
	 * @return
	 */
	public Long getNewOpportunityCount(HomeStatisticSearchWrapperRequest request) {
		return extOpportunityMapper.selectOpportunityCount(request, false);
	}

	/**
	 * 获取未跟进客户统计
	 * @param request
	 * @return
	 */
	public Long getOpportunityTotalAmount(HomeStatisticSearchWrapperRequest request) {
		HomeStatisticSearchWrapperRequest totalRequest = copyHomeStatisticSearchWrapperRequest(request);
		totalRequest.clearStartTimeAndEndTie();
		return extOpportunityMapper.selectOpportunityCount(totalRequest, true);
	}

	/**
	 * 获取统计数量和较上期对比率的通用方法
	 * @param request
	 * @param statisticFunction
	 * @return
	 */
	public HomeStatisticSearchResponse getStatisticSearchResponse(HomeStatisticSearchWrapperRequest request,
																		  Function<HomeStatisticSearchWrapperRequest, Long> statisticFunction) {
		HomeStatisticSearchResponse response = new HomeStatisticSearchResponse();
		Future<Long> getCount = executor.submit(() -> statisticFunction.apply(request));
		try {
			if (request.comparePeriod()) {
				HomeStatisticSearchWrapperRequest periodRequest = copyHomeStatisticSearchWrapperRequest(request);
				periodRequest.setStartTime(periodRequest.getPeriodStartTime());
				periodRequest.setEndTime(periodRequest.getPeriodEndTime());
				Future<Long> getPeriodCount = executor.submit(() -> statisticFunction.apply(periodRequest));

				Long count = getCount.get();
				response.setValue(count);
				Long periodCount = getPeriodCount.get();
				response.setPriorPeriodCompareRate(getPriorPeriodCompareRate(count, periodCount));
			} else {
				Long count = getCount.get();
				response.setValue(count);
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
	 * @param request
	 * @param permission
	 * @return
	 */
	public DeptDataPermissionDTO getDeptDataPermissionDTO(HomeStatisticSearchRequest request, String permission) {
		if (StringUtils.equals(request.getSearchType(), BusinessSearchType.DEPARTMENT.name())) {
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
		} else if (StringUtils.equals(request.getSearchType(), BusinessSearchType.ALL.name())) {
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

		if (hasAllPermission || StringUtils.equals(userId, InternalUser.ADMIN.getValue())) {
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

	public HomeStatisticSearchWrapperRequest getHomeStatisticSearchWrapperRequest(HomeStatisticSearchRequest request) {
		DeptDataPermissionDTO deptDataPermission = getDeptDataPermissionDTO(request, PermissionConstants.CUSTOMER_MANAGEMENT_READ);
		HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(request, deptDataPermission, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
		return wrapperRequest;
	}

	public boolean isEmptyDeptData(HomeStatisticSearchWrapperRequest wrapperRequest) {
		return StringUtils.equals(wrapperRequest.getStaticRequest().getSearchType(), BusinessSearchType.DEPARTMENT.name())
				&& CollectionUtils.isEmpty(wrapperRequest.getDataPermission().getDeptIds());
	}
}