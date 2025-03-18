package io.cordys.crm.customer.service;

import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.customer.domain.CustomerCollaboration;
import io.cordys.crm.customer.dto.request.CustomerCollaborationAddRequest;
import io.cordys.crm.customer.dto.request.CustomerCollaborationUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerCollaborationListResponse;
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
public class CustomerCollaborationService {
    @Resource
    private BaseMapper<CustomerCollaboration> customerCollaborationMapper;
    @Resource
    private BaseService baseService;

    public List<CustomerCollaborationListResponse> list(String customerId, String orgId) {
        CustomerCollaboration example = new CustomerCollaboration();
        example.setCustomerId(customerId);
        List<CustomerCollaboration> collaborations = customerCollaborationMapper.select(example);
        return buildListData(orgId, collaborations);
    }

    public List<CustomerCollaboration> selectByCustomerIdAndUserId(String customerId, String userId) {
        CustomerCollaboration example = new CustomerCollaboration();
        example.setCustomerId(customerId);
        example.setUserId(userId);
        return customerCollaborationMapper.select(example);
    }

    private List<CustomerCollaborationListResponse> buildListData(String orgId, List<CustomerCollaboration> collaborations) {
        if (CollectionUtils.isEmpty(collaborations)) {
            return List.of();
        }
        Set<String> userIds = new HashSet<>();
        Set<String> ownerIds = new HashSet<>();

        for (CustomerCollaboration collaboration : collaborations) {
            userIds.add(collaboration.getCreateUser());
            userIds.add(collaboration.getUpdateUser());
            userIds.add(collaboration.getUserId());
            ownerIds.add(collaboration.getUserId());
        }

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(new ArrayList<>(ownerIds), orgId);

        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        return collaborations
                .stream()
                .map(item -> {
                    CustomerCollaborationListResponse customerCollaboration =
                            BeanUtils.copyBean(new CustomerCollaborationListResponse(), item);
                    UserDeptDTO userDeptDTO = userDeptMap.get(customerCollaboration.getUserId());
                    if (userDeptMap != null) {
                        customerCollaboration.setDepartmentId(userDeptDTO.getDeptId());
                        customerCollaboration.setDepartmentName(userDeptDTO.getDeptName());
                    }
                    customerCollaboration.setCreateUserName(userNameMap.get(customerCollaboration.getCreateUser()));
                    customerCollaboration.setUpdateUserName(userNameMap.get(customerCollaboration.getUpdateUser()));
                    customerCollaboration.setUserName(userNameMap.get(customerCollaboration.getUserId()));
                    return customerCollaboration;
                }).toList();
    }

    public CustomerCollaboration add(CustomerCollaborationAddRequest request, String userId) {
        CustomerCollaboration customerCollaboration = BeanUtils.copyBean(new CustomerCollaboration(), request);
        customerCollaboration.setCreateTime(System.currentTimeMillis());
        customerCollaboration.setUpdateTime(System.currentTimeMillis());
        customerCollaboration.setUpdateUser(userId);
        customerCollaboration.setCreateUser(userId);
        customerCollaboration.setId(IDGenerator.nextStr());
        customerCollaborationMapper.insert(customerCollaboration);
        return customerCollaborationMapper.selectByPrimaryKey(customerCollaboration.getId());
    }

    public CustomerCollaboration update(CustomerCollaborationUpdateRequest request, String userId) {
        CustomerCollaboration customerCollaboration = BeanUtils.copyBean(new CustomerCollaboration(), request);
        customerCollaboration.setUpdateTime(System.currentTimeMillis());
        customerCollaboration.setUpdateUser(userId);
        customerCollaborationMapper.update(customerCollaboration);
        return customerCollaborationMapper.selectByPrimaryKey(customerCollaboration.getId());
    }

    public void delete(String id) {
        customerCollaborationMapper.deleteByPrimaryKey(id);
    }

    public void batchDelete(List<String> ids) {
        for (String id : ids) {
            // 数据少，直接循环删
            customerCollaborationMapper.deleteByPrimaryKey(id);
        }
    }

    public void deleteByCustomerId(String customerId) {
        CustomerCollaboration example = new CustomerCollaboration();
        example.setCustomerId(customerId);
        customerCollaborationMapper.delete(example);
    }

    public void deleteByCustomerIds(List<String> customerIds) {
        LambdaQueryWrapper<CustomerCollaboration> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CustomerCollaboration::getCustomerId, customerIds);
        customerCollaborationMapper.deleteByLambda(wrapper);
    }
}