package io.cordys.crm.customer.service;

import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerOwner;
import io.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import io.cordys.crm.customer.dto.response.CustomerOwnerListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerOwnerMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerOwnerHistoryService {
    @Resource
    private BaseMapper<CustomerOwner> customerOwnerMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ExtCustomerOwnerMapper extCustomerOwnerMapper;

    public List<CustomerOwnerListResponse> list(String customerId, String orgId) {
        CustomerOwner example = new CustomerOwner();
        example.setCustomerId(customerId);
        List<CustomerOwner> owners = customerOwnerMapper.select(example);
        return buildListData(orgId, owners);
    }

    private List<CustomerOwnerListResponse> buildListData(String orgId, List<CustomerOwner> owners) {
        if (CollectionUtils.isEmpty(owners)) {
            return List.of();
        }
        Set<String> userIds = new HashSet<>();
        Set<String> ownerIds = new HashSet<>();

        for (CustomerOwner owner : owners) {
            userIds.add(owner.getOwner());
            userIds.add(owner.getOperator());
            ownerIds.add(owner.getOwner());
        }

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(new ArrayList<>(ownerIds), orgId);

        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        return owners
                .stream()
                .map(item -> {
                    CustomerOwnerListResponse customerOwner =
                            BeanUtils.copyBean(new CustomerOwnerListResponse(), item);
                    UserDeptDTO userDeptDTO = userDeptMap.get(customerOwner.getOwner());
                    if (userDeptMap != null) {
                        customerOwner.setDepartmentId(userDeptDTO.getDeptId());
                        customerOwner.setDepartmentName(userDeptDTO.getDeptName());
                    }
                    customerOwner.setOwnerName(userNameMap.get(customerOwner.getOwner()));
                    customerOwner.setOperatorName(userNameMap.get(customerOwner.getOperator()));
                    return customerOwner;
                }).toList();
    }

    public CustomerOwner add(Customer customer, String userId) {
        CustomerOwner customerOwner = new CustomerOwner();
        customerOwner.setOwner(customer.getOwner());
        customerOwner.setOperator(userId);
        customerOwner.setCustomerId(customer.getId());
        customerOwner.setCollectionTime(customer.getCollectionTime());
        customerOwner.setEndTime(System.currentTimeMillis());
        customerOwner.setId(IDGenerator.nextStr());
        customerOwnerMapper.insert(customerOwner);
        return customerOwner;
    }

    public void batchAdd(CustomerBatchTransferRequest transferRequest, String userId) {
        extCustomerOwnerMapper.batchAdd(transferRequest, userId);
    }

    public void deleteByCustomerIds(List<String> customerIds) {
        LambdaQueryWrapper<CustomerOwner> wrapper = new LambdaQueryWrapper();
        wrapper.in(CustomerOwner::getCustomerId, customerIds);
        customerOwnerMapper.deleteByLambda(wrapper);
    }
}