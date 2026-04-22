package cn.cordys.crm.system.job.listener;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerPool;
import cn.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerOwnerHistoryService;
import cn.cordys.crm.customer.service.CustomerPoolService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户池回收监听器
 */
@Component
@Slf4j
public class CustomerPoolRecycleListener extends AbstractPoolRecycleListener<CustomerPool, Customer, CustomerPoolRecycleRule> {

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
    protected List<CustomerPool> getEnabledPools() {
        LambdaQueryWrapper<CustomerPool> qw = new LambdaQueryWrapper<CustomerPool>()
                .eq(CustomerPool::getEnable, true)
                .eq(CustomerPool::getAuto, true);
        return customerPoolMapper.selectListByLambda(qw);
    }

    @Override
    protected Map<List<String>, CustomerPool> getOwnersBestMatchPoolMap(List<CustomerPool> pools) {
        return customerPoolService.getOwnersBestMatchPoolMap(pools);
    }

    @Override
    protected List<Customer> getEntitiesForRecycle(List<String> ownerIds) {
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<Customer>()
                .in(Customer::getOwner, ownerIds)
                .eq(Customer::getInSharedPool, false);
        return customerMapper.selectListByLambda(qw);
    }

    @Override
    protected Map<String, CustomerPoolRecycleRule> getRecycleRules(List<CustomerPool> pools) {
        List<String> poolIds = pools.stream().map(CustomerPool::getId).toList();
        LambdaQueryWrapper<CustomerPoolRecycleRule> qw = new LambdaQueryWrapper<CustomerPoolRecycleRule>()
                .in(CustomerPoolRecycleRule::getPoolId, poolIds);
        return customerPoolRecycleRuleMapper.selectListByLambda(qw).stream()
                .collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, r -> r));
    }

    @Override
    protected boolean checkRecycled(Customer customer, CustomerPoolRecycleRule rule) {
        return customerPoolService.checkRecycled(customer, rule);
    }

    @Override
    protected void processEntityRecycle(Customer customer, CustomerPool pool) {
        customerContactService.updateContactOwner(
                customer.getId(), "-", customer.getOwner(), customer.getOrganizationId()
        );

        commonNoticeSendService.sendNotice(
                NotificationConstants.Module.CUSTOMER,
                NotificationConstants.Event.CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS,
                customer.getName(),
                InternalUser.ADMIN.getValue(),
                customer.getOrganizationId(),
                List.of(customer.getOwner()),
                true
        );

        customerOwnerHistoryService.add(customer, InternalUser.ADMIN.getValue(), false);

        customer.setPoolId(pool.getId());
        customer.setInSharedPool(true);
        customer.setOwner(null);
        customer.setCollectionTime(null);
        customer.setReasonId("system");
        customer.setUpdateUser(InternalUser.ADMIN.getValue());
        customer.setUpdateTime(System.currentTimeMillis());

        extCustomerMapper.moveToPool(customer);
    }

    @Override
    protected String getEntityDescription() {
        return "客户";
    }

    @Override
    protected CommonNoticeSendService getNoticeSendService() {
        return commonNoticeSendService;
    }

    @Override
    protected String getOwner(Customer customer) {
        return customer.getOwner();
    }

    @Override
    protected String getPoolId(CustomerPool pool) {
        return pool.getId();
    }
}
