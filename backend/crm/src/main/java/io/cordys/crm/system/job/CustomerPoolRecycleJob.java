package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.customer.service.CustomerOwnerHistoryService;
import io.cordys.crm.customer.service.CustomerPoolService;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.notice.CommonNoticeSendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomerPoolRecycleJob {

	@Resource
	private BaseMapper<Customer> customerMapper;
	@Resource
	private BaseMapper<CustomerPool> customerPoolMapper;
	@Resource
	private BaseMapper<CustomerPoolRecycleRule> customerPoolRecycleRuleMapper;
	@Resource
	private ExtCustomerMapper extCustomerMapper;
	@Resource
	private CustomerPoolService customerPoolService;
	@Resource
	private CustomerOwnerHistoryService customerOwnerHistoryService;
	@Resource
	private CommonNoticeSendService commonNoticeSendService;

	/**
	 * 回收客户
	 */
	@QuartzScheduled(cron = "0 0/5 * * * ?")
	public void recycle() {
		LogUtils.info("Start recycle customer resource");
		LambdaQueryWrapper<CustomerPool> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(CustomerPool::getEnable, true).eq(CustomerPool::getAuto, true);
		List<CustomerPool> pools = customerPoolMapper.selectListByLambda(queryWrapper);
		if (CollectionUtils.isEmpty(pools)) {
			return;
		}
		Map<List<String>, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersBestMatchPoolMap(pools);
		List<String> recycleOwnersIds = ownersDefaultPoolMap.keySet().stream().flatMap(List::stream).toList();
		LambdaQueryWrapper<Customer> customerQueryWrapper = new LambdaQueryWrapper<>();
		customerQueryWrapper.in(Customer::getOwner, recycleOwnersIds).eq(Customer::getInSharedPool, false);
		List<Customer> customers = customerMapper.selectListByLambda(customerQueryWrapper);
		if (CollectionUtils.isEmpty(customers)) {
			return;
		}
		List<String> poolIds = pools.stream().map(CustomerPool::getId).toList();
		LambdaQueryWrapper<CustomerPoolRecycleRule> ruleQueryWrapper = new LambdaQueryWrapper<>();
		ruleQueryWrapper.in(CustomerPoolRecycleRule::getPoolId, poolIds);
		List<CustomerPoolRecycleRule> recycleRules = customerPoolRecycleRuleMapper.selectListByLambda(ruleQueryWrapper);
		Map<String, CustomerPoolRecycleRule> recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, r -> r));
		customers.forEach(customer -> ownersDefaultPoolMap.forEach((ownerIds, pool) -> {
			if (ownerIds.contains(customer.getOwner())) {
				CustomerPoolRecycleRule rule = recycleRuleMap.get(pool.getId());
				boolean recycle = customerPoolService.checkRecycled(customer, rule);
				if (recycle) {
					// 消息通知
					commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
							NotificationConstants.Event.CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS, customer.getName(), InternalUser.ADMIN.getValue(), 
							customer.getOrganizationId(), List.of(customer.getOwner()), true);
					// 插入责任人历史
					customerOwnerHistoryService.add(customer, InternalUser.ADMIN.getValue());
					customer.setPoolId(pool.getId());
					customer.setInSharedPool(true);
					customer.setOwner(null);
					customer.setCollectionTime(null);
					customer.setUpdateUser(InternalUser.ADMIN.getValue());
					customer.setUpdateTime(System.currentTimeMillis());
					// 回收客户至公海
					extCustomerMapper.moveToPool(customer);
				}
			}
		}));
		LogUtils.info("Recycle customer resource done");
	}
}
