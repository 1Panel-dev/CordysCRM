package io.cordys.crm.home.service;

import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.constants.RoleDataScope;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.RoleDataScopeDTO;
import io.cordys.common.dto.RolePermissionDTO;
import io.cordys.common.permission.PermissionCache;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.util.LogUtils;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.home.dto.request.HomeStatisticSearchRequest;
import io.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import io.cordys.crm.home.dto.response.HomeClueStatistic;
import io.cordys.crm.home.dto.response.HomeCustomerStatistic;
import io.cordys.crm.home.dto.response.HomeOpportunityStatistic;
import io.cordys.crm.home.dto.response.HomeStatisticSearchResponse;
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
	private DataScopeService dataScopeService;
	@Resource
	private PermissionCache permissionCache;
	@Resource
	private DepartmentService departmentService;
	@Resource
	private RoleService roleService;

	public HomeCustomerStatistic getCustomerStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeCustomerStatistic customerStatistic = new HomeCustomerStatistic();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			// 多线程执行
			Future<HomeStatisticSearchResponse> getNewCustomerStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getNewCustomerCount));
			Future<HomeStatisticSearchResponse> getUnfollowedCustomerStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getUnfollowedCustomerCount));
			Future<Long> getTotalCustomerCount = executor.submit(() -> getTotalCustomerCount(request));

			customerStatistic.setNewCustomer(getNewCustomerStatistic.get());
			customerStatistic.setUnfollowedCustomer(getUnfollowedCustomerStatistic.get());
			customerStatistic.setTotal(getTotalCustomerCount.get());
		} catch (Exception e) {
			LogUtils.error(e);
		}
		return customerStatistic;
	}

	public HomeClueStatistic getClueStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeClueStatistic clueStatistic = new HomeClueStatistic();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			// 多线程执行
			Future<HomeStatisticSearchResponse> getNewClueStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getNewClueCount));
			Future<HomeStatisticSearchResponse> getUnfollowedClueStatistic = executor.submit(() ->
					getStatisticSearchResponse(request, this::getUnfollowedClueCount));
			Future<Long> getTotalClueCount = executor.submit(() -> getTotalClueCount(request));

			clueStatistic.setNewClue(getNewClueStatistic.get());
			clueStatistic.setUnfollowedClue(getUnfollowedClueStatistic.get());
			clueStatistic.setTotal(getTotalClueCount.get());
		} catch (Exception e) {
			LogUtils.error(e);
		}
		return clueStatistic;
	}


	public HomeOpportunityStatistic getOpportunityStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeOpportunityStatistic opportunityStatistic = new HomeOpportunityStatistic();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
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

	/**
	 * 获取客户总数
	 * @param request
	 * @return
	 */
	public Long getTotalCustomerCount(HomeStatisticSearchWrapperRequest request) {
		HomeStatisticSearchWrapperRequest totalRequest = new HomeStatisticSearchWrapperRequest(request.getStaticRequest(), request.getDataPermission(), request.getOrgId());
		totalRequest.setStartTime(null);
		totalRequest.setEndTime(null);
		return extCustomerMapper.selectCustomerCount(totalRequest, false);
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
		HomeStatisticSearchWrapperRequest totalRequest = new HomeStatisticSearchWrapperRequest(request.getStaticRequest(), request.getDataPermission(), request.getOrgId());
		totalRequest.setStartTime(null);
		totalRequest.setEndTime(null);
		return extClueMapper.selectClueCount(totalRequest, false);
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
		HomeStatisticSearchWrapperRequest totalRequest = new HomeStatisticSearchWrapperRequest(request.getStaticRequest(), request.getDataPermission(), request.getOrgId());
		totalRequest.setStartTime(null);
		totalRequest.setEndTime(null);
		return extOpportunityMapper.selectOpportunityCount(totalRequest, false);
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
		return extOpportunityMapper.selectOpportunityCount(request, true);
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
		Long count = statisticFunction.apply(request);
		response.setValue(count);
		if (request.comparePeriod()) {
			HomeStatisticSearchWrapperRequest periodRequest = new HomeStatisticSearchWrapperRequest(request.getStaticRequest(), request.getDataPermission(), request.getOrgId());
			periodRequest.setStartTime(periodRequest.getPeriodStartTime());
			periodRequest.setEndTime(periodRequest.getPeriodEndTime());
			Long periodCount = statisticFunction.apply(periodRequest);
			response.setPriorPeriodCompareRate(getPriorPeriodCompareRate(count, periodCount));
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
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
				OrganizationContext.getOrganizationId(), request.getSearchType(), permission);
		if (CollectionUtils.isNotEmpty(request.getDeptIds())) {
			Set<String> deptIds = request.getDeptIds()
					.stream()
					.filter(deptId -> CollectionUtils.isNotEmpty(deptDataPermission.getDeptIds())
							&& deptDataPermission.getDeptIds().contains(deptId))
					.collect(Collectors.toSet());
			deptDataPermission.setDeptIds(deptIds);
		}
		return deptDataPermission;
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
		HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(request, deptDataPermission, OrganizationContext.getOrganizationId());
		return wrapperRequest;
	}
}