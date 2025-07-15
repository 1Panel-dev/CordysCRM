package io.cordys.crm.home.service;

import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.util.BeanUtils;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.home.dto.request.HomeStatisticSearchRequest;
import io.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import io.cordys.crm.home.dto.response.HomeCustomerStatistic;
import io.cordys.crm.home.dto.response.HomeStatisticSearchResponse;
import io.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class HomeStatisticService {

	@Resource
	private ExtCustomerMapper extCustomerMapper;
	@Resource
	private DataScopeService dataScopeService;

	public HomeCustomerStatistic getCustomerStatistic(HomeStatisticSearchWrapperRequest request) {
		HomeCustomerStatistic customerStatistic = new HomeCustomerStatistic();
		customerStatistic.setNewCustomer(getNewCustomerStatistic(request));
		customerStatistic.setTotal(getTotalCustomerCount(request));
		return customerStatistic;
	}

	/**
	 * 获取客户总数
	 * @param request
	 * @return
	 */
	public Long getTotalCustomerCount(HomeStatisticSearchWrapperRequest request) {
		HomeStatisticSearchWrapperRequest totalRequest = new HomeStatisticSearchWrapperRequest(request.getStaticRequest(), request.getDataPermission());
		totalRequest.setStartTime(null);
		totalRequest.setEndTime(null);
		return extCustomerMapper.selectCustomerStatistic(totalRequest);
	}

	/**
	 * 获取新增客户统计
	 * @param request
	 * @return
	 */
	public HomeStatisticSearchResponse getNewCustomerStatistic(HomeStatisticSearchWrapperRequest request) {
		return getCustomerStatisticSearchResponse(request, extCustomerMapper::selectCustomerStatistic);
	}

	/**
	 * 获取统计数量和较上期对比率的通用方法
	 * @param request
	 * @param statisticFunction
	 * @return
	 */
	public HomeStatisticSearchResponse getCustomerStatisticSearchResponse(HomeStatisticSearchWrapperRequest request,
																		  Function<HomeStatisticSearchWrapperRequest, Long> statisticFunction) {
		HomeStatisticSearchResponse response = new HomeStatisticSearchResponse();
		Long count = statisticFunction.apply(request);
		response.setValue(count);
		if (request.comparePeriod()) {
			HomeStatisticSearchWrapperRequest periodRequest = new HomeStatisticSearchWrapperRequest(request.getStaticRequest(), request.getDataPermission());
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
}