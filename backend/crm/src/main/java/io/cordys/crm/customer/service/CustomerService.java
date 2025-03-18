package io.cordys.crm.customer.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.customer.constants.CustomerResultCode;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.CustomerUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerGetResponse;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerService {

    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private CustomerFieldService customerFieldService;
    @Resource
    private CustomerCollaborationService customerCollaborationService;
    @Resource
    private CustomerOwnerHistoryService customerOwnerHistoryService;
    @Resource
    private CustomerPoolService customerPoolService;
    @Resource
    private BaseMapper<CustomerPoolRecycleRule> customerPoolRecycleRuleMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private CustomerRelationService customerRelationService;

    public PagerWithOption<List<CustomerListResponse>> list(CustomerPageRequest request, String userId, String orgId,
                                                            DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomerListResponse> list = extCustomerMapper.list(request, orgId, userId, deptDataPermission);
        List<CustomerListResponse> buildList = buildListData(list, orgId);

        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, CustomerListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        moduleFormService.putBusinessFieldOption(buildList, customerFormConfig.getFields(), BusinessModuleField.CUSTOMER_OWNER,
                CustomerListResponse::getOwner, CustomerListResponse::getOwnerName, optionMap);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    private List<CustomerListResponse> buildListData(List<CustomerListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> customerIds = list.stream().map(CustomerListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = customerFieldService.getResourceFieldMap(customerIds);

        List<String> ownerIds = list.stream()
                .map(CustomerListResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = list.stream()
                .map(CustomerListResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = list.stream()
                .map(CustomerListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(CustomerListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        // 获取负责人默认公海信息
        Map<String, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        List<String> poolIds = ownersDefaultPoolMap.values().stream().map(CustomerPool::getId).distinct().toList();
        Map<String, CustomerPoolRecycleRule> recycleRuleMap;
        if (CollectionUtils.isEmpty(poolIds)) {
            recycleRuleMap = Map.of();
        } else {
            LambdaQueryWrapper<CustomerPoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
            recycleRuleWrapper.in(CustomerPoolRecycleRule::getPoolId, poolIds);
            List<CustomerPoolRecycleRule> recycleRules = customerPoolRecycleRuleMapper.selectListByLambda(recycleRuleWrapper);
            recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, rule -> rule));
        }

        list.forEach(customerListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            customerListResponse.setModuleFields(customerFields);
            // 设置回收公海
            CustomerPool reservePool = ownersDefaultPoolMap.get(customerListResponse.getOwner());
            customerListResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            // 计算剩余归属天数
            customerListResponse.setReservedDays(customerPoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    customerListResponse));

            UserDeptDTO userDeptDTO = userDeptMap.get(customerListResponse.getOwner());
            if (userDeptDTO != null) {
                customerListResponse.setDepartmentId(userDeptDTO.getDeptId());
                customerListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            customerListResponse.setFollowerName(userNameMap.get(customerListResponse.getFollower()));
            customerListResponse.setCreateUserName(userNameMap.get(customerListResponse.getCreateUser()));
            customerListResponse.setUpdateUserName(userNameMap.get(customerListResponse.getUpdateUser()));
            customerListResponse.setOwnerName(userNameMap.get(customerListResponse.getOwner()));
        });

        return list;
    }

    public CustomerGetResponse get(String id, String orgId) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        CustomerGetResponse customerGetResponse = BeanUtils.copyBean(new CustomerGetResponse(), customer);

        // 获取模块字段
        List<BaseModuleFieldValue> customerFields = customerFieldService.getModuleFieldValuesByResourceId(id);
        customerGetResponse.setModuleFields(customerFields);

        UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(customerGetResponse.getOwner(), orgId);
        if (userDeptDTO != null) {
            customerGetResponse.setDepartmentId(userDeptDTO.getDeptId());
            customerGetResponse.setDepartmentName(userDeptDTO.getDeptName());
        }

        return baseService.setCreateUpdateOwnerUserName(customerGetResponse);
    }

    public Customer add(CustomerAddRequest request, String userId, String orgId) {
        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setCreateTime(System.currentTimeMillis());
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setCollectionTime(customer.getCreateTime());
        customer.setUpdateUser(userId);
        customer.setCreateUser(userId);
        customer.setOrganizationId(orgId);
        customer.setId(IDGenerator.nextStr());
        customer.setInSharedPool(false);

        // 校验名称重复
        checkAddExist(customer);

        customerMapper.insert(customer);

        //保存自定义字段
        customerFieldService.saveModuleField(customer.getId(), request.getModuleFields());
        return customer;
    }

    public Customer update(CustomerUpdateRequest request, String userId) {
        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser(userId);

        // 校验名称重复
        checkUpdateExist(customer);

        if (StringUtils.isNotBlank(request.getOwner())) {
            Customer originCustomer = customerMapper.selectByPrimaryKey(request.getId());
            if (!StringUtils.equals(request.getOwner(), originCustomer.getOwner())) {
                // 如果责任人有修改，则添加责任人历史
                customerOwnerHistoryService.add(originCustomer, userId);
            }
        }

        customerMapper.update(customer);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields());
        return customerMapper.selectByPrimaryKey(customer.getId());
    }

    private void updateModuleField(String customerId, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        customerFieldService.deleteByResourceId(customerId);
        // 再保存
        customerFieldService.saveModuleField(customerId, moduleFields);
    }

    private void checkAddExist(Customer customer) {
        if (extCustomerMapper.checkAddExist(customer)) {
            throw new GenericException(CustomerResultCode.CUSTOMER_EXIST);
        }
    }

    private void checkUpdateExist(Customer customer) {
        if (extCustomerMapper.checkUpdateExist(customer)) {
            throw new GenericException(CustomerResultCode.CUSTOMER_EXIST);
        }
    }

    public void delete(String id) {
        // 删除客户
        customerMapper.deleteByPrimaryKey(id);
        // 删除客户模块字段
        customerFieldService.deleteByResourceId(id);
        // 删除客户协作人
        customerCollaborationService.deleteByCustomerId(id);
        // 删除责任人历史
        customerOwnerHistoryService.deleteByCustomerIds(List.of(id));
        // 删除客户关系
        customerRelationService.deleteByCustomerId(id);
    }

    public void batchTransfer(CustomerBatchTransferRequest request, String userId) {
        extCustomerMapper.batchTransfer(request);
        // 添加责任人历史
        customerOwnerHistoryService.batchAdd(request, userId);
    }

    public void batchDelete(List<String> ids) {
        // 删除客户
        customerMapper.deleteByIds(ids);
        // 删除客户模块字段
        customerFieldService.deleteByResourceIds(ids);
        // 删除客户协作人
        customerCollaborationService.deleteByCustomerIds(ids);
        // 删除责任人历史
        customerOwnerHistoryService.deleteByCustomerIds(ids);
        // 删除客户关系
        customerRelationService.deleteByCustomerIds(ids);
    }

    /**
     * 批量移入公海
     *
     * @param ids         id集合
     * @param orgId       组织ID
     * @param currentUser 当前用户
     */
    public void batchToPool(List<String> ids, String currentUser, String orgId) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Customer::getId, ids);
        List<Customer> customers = customerMapper.selectListByLambda(wrapper);
        List<String> ownerIds = customers.stream().map(Customer::getOwner).distinct().toList();
        Map<String, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        for (Customer customer : customers) {
            CustomerPool customerPool = ownersDefaultPoolMap.get(customer.getOwner());
            if (customerPool == null) {
                // 未找到默认公海，不移入
                continue;
            }
            // 插入责任人历史
            customerOwnerHistoryService.add(customer, currentUser);
            customer.setPoolId(customerPool.getId());
            customer.setInSharedPool(true);
            customer.setOwner(null);
            customer.setCollectionTime(null);
            customer.setUpdateUser(currentUser);
            customer.setUpdateTime(System.currentTimeMillis());
            customerMapper.updateById(customer);
        }
    }
}