package io.cordys.crm.system.job.listener;

import io.cordys.common.constants.InternalUser;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.customer.service.CustomerContactService;
import io.cordys.crm.customer.service.CustomerOwnerHistoryService;
import io.cordys.crm.customer.service.CustomerPoolService;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.notice.CommonNoticeSendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户池回收任务
 * <p>
 * 该定时任务负责根据设定的规则自动将符合条件的客户从个人池回收到公共客户池。
 * 回收规则基于CustomerPoolRecycleRule中定义的条件，如最后跟进时间、最后更新时间等。
 * </p>
 */
@Component
public class CustomerPoolRecycleListener implements ApplicationListener<ExecuteEvent> {

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
    @Resource
    private CustomerContactService customerContactService;


    @Override
    public void onApplicationEvent(@NotNull ExecuteEvent event) {
        try {
            this.recycle();
        } catch (Exception e) {
            LogUtils.error("回收客户资源异常: ", e.getMessage());
        }
    }

    /**
     * 执行客户回收
     * <p>
     * 回收流程：
     * 1. 获取启用了自动回收的客户池
     * 2. 找出符合回收规则的客户
     * 3. 执行回收操作并发送通知
     * </p>
     */
    public void recycle() {
        LogUtils.info("开始回收客户资源");

        // 获取启用了自动回收的客户池
        List<CustomerPool> pools = getEnabledAutomaticPools();
        if (CollectionUtils.isEmpty(pools)) {
            LogUtils.info("没有启用的自动回收公海，回收任务结束");
            return;
        }

        // 获取池与负责人的映射关系
        Map<List<String>, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersBestMatchPoolMap(pools);
        List<String> recycleOwnersIds = ownersDefaultPoolMap.keySet().stream().flatMap(List::stream).toList();

        // 查询符合条件的客户
        List<Customer> customers = getCustomersForRecycle(recycleOwnersIds);
        if (CollectionUtils.isEmpty(customers)) {
            LogUtils.info("没有需要回收的客户，回收任务结束");
            return;
        }

        // 获取回收规则
        Map<String, CustomerPoolRecycleRule> recycleRuleMap = getPoolRecycleRules(pools);

        // 执行回收操作
        recycleCustomers(customers, ownersDefaultPoolMap, recycleRuleMap);

        LogUtils.info("客户资源回收完成");
    }

    /**
     * 获取已启用且设置为自动回收的客户池
     *
     * @return 符合条件的客户池列表
     */
    private List<CustomerPool> getEnabledAutomaticPools() {
        LambdaQueryWrapper<CustomerPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomerPool::getEnable, true).eq(CustomerPool::getAuto, true);
        return customerPoolMapper.selectListByLambda(queryWrapper);
    }

    /**
     * 获取需要被考虑回收的客户列表
     *
     * @param recycleOwnersIds 需要考虑回收的负责人ID列表
     * @return 符合初步条件的客户列表
     */
    private List<Customer> getCustomersForRecycle(List<String> recycleOwnersIds) {
        LambdaQueryWrapper<Customer> customerQueryWrapper = new LambdaQueryWrapper<>();
        customerQueryWrapper.in(Customer::getOwner, recycleOwnersIds).eq(Customer::getInSharedPool, false);
        return customerMapper.selectListByLambda(customerQueryWrapper);
    }

    /**
     * 获取客户池回收规则
     *
     * @param pools 客户池列表
     * @return 池ID到回收规则的映射
     */
    private Map<String, CustomerPoolRecycleRule> getPoolRecycleRules(List<CustomerPool> pools) {
        List<String> poolIds = pools.stream().map(CustomerPool::getId).toList();
        LambdaQueryWrapper<CustomerPoolRecycleRule> ruleQueryWrapper = new LambdaQueryWrapper<>();
        ruleQueryWrapper.in(CustomerPoolRecycleRule::getPoolId, poolIds);
        List<CustomerPoolRecycleRule> recycleRules = customerPoolRecycleRuleMapper.selectListByLambda(ruleQueryWrapper);
        return recycleRules.stream().collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, r -> r));
    }

    /**
     * 执行客户回收操作
     *
     * @param customers            待检查回收的客户列表
     * @param ownersDefaultPoolMap 负责人与池的映射关系
     * @param recycleRuleMap       池ID与回收规则的映射
     */
    private void recycleCustomers(List<Customer> customers, Map<List<String>, CustomerPool> ownersDefaultPoolMap,
                                  Map<String, CustomerPoolRecycleRule> recycleRuleMap) {
        customers.forEach(customer -> ownersDefaultPoolMap.forEach((ownerIds, pool) -> {
            if (ownerIds.contains(customer.getOwner())) {
                CustomerPoolRecycleRule rule = recycleRuleMap.get(pool.getId());
                boolean recycle = customerPoolService.checkRecycled(customer, rule);
                if (recycle) {
                    processCustomerRecycle(customer, pool);
                }
            }
        }));
    }

    /**
     * 处理单个客户的回收流程
     *
     * @param customer 需要回收的客户
     * @param pool     目标客户池
     */
    private void processCustomerRecycle(Customer customer, CustomerPool pool) {
        //更新责任人
        customerContactService.updateContactOwner(customer.getId(), "-", customer.getOwner(), customer.getOrganizationId());

        // 消息通知
        commonNoticeSendService.sendNotice(
                NotificationConstants.Module.CUSTOMER,
                NotificationConstants.Event.CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS,
                customer.getName(),
                InternalUser.ADMIN.getValue(),
                customer.getOrganizationId(),
                List.of(customer.getOwner()),
                true
        );

        // 插入责任人历史
        customerOwnerHistoryService.add(customer, InternalUser.ADMIN.getValue());

        // 更新客户信息
        customer.setPoolId(pool.getId());
        customer.setInSharedPool(true);
        customer.setOwner(null);
        customer.setCollectionTime(null);
        customer.setReasonId("system");
        customer.setUpdateUser(InternalUser.ADMIN.getValue());
        customer.setUpdateTime(System.currentTimeMillis());

        // 回收客户至公海
        extCustomerMapper.moveToPool(customer);
    }
}