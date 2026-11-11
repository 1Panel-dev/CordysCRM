package cn.cordys.crm.customer.service;

import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.constants.CustomerCollaborationType;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.dto.request.CustomerCollaborationAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerCollaborationUpdateRequest;
import cn.cordys.crm.customer.dto.response.CustomerCollaborationListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerCollaborationMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jianxing
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerCollaborationService {
    @Resource
    private CustomerAssociationPermissionService associationPermissionService;
    @Resource
    private BaseMapper<CustomerCollaboration> customerCollaborationMapper;
    @Resource
    private ExtCustomerCollaborationMapper extCustomerCollaborationMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private BaseMapper<Customer> customerMapper;

    public List<CustomerCollaborationListResponse> list(String customerId, String orgId) {
        CustomerCollaboration example = new CustomerCollaboration();
        example.setCustomerId(customerId);
        List<CustomerCollaboration> collaborations = customerCollaborationMapper.select(example);
        return buildListData(orgId, effective(collaborations));
    }

    public List<CustomerCollaboration> selectByCustomerIdAndUserId(String customerId, String userId) {
        CustomerCollaboration example = new CustomerCollaboration();
        example.setCustomerId(customerId);
        example.setUserId(userId);
        return effective(customerCollaborationMapper.select(example));
    }

    public List<CustomerCollaboration> selectByCustomerId(String customerId) {
        CustomerCollaboration example = new CustomerCollaboration();
        example.setCustomerId(customerId);
        return effective(customerCollaborationMapper.select(example));
    }

    private List<CustomerCollaboration> effective(List<CustomerCollaboration> collaborations) {
        long now = System.currentTimeMillis();
        return collaborations.stream().map(item -> {
            if (CustomerCollaborationType.JOINT_FOLLOW.name().equals(item.getCollaborationType())
                    && item.getExpireTime() != null && item.getExpireTime() <= now) {
                item.setCollaborationType(CustomerCollaborationType.COLLABORATION.name());
            }
            return item;
        }).toList();
    }

    private List<CustomerCollaborationListResponse> buildListData(String orgId,
            List<CustomerCollaboration> collaborations) {
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
                    CustomerCollaborationListResponse customerCollaboration = BeanUtils
                            .copyBean(new CustomerCollaborationListResponse(), item);
                    UserDeptDTO userDeptDTO = userDeptMap.get(customerCollaboration.getUserId());
                    if (userDeptDTO != null) {
                        customerCollaboration.setDepartmentId(userDeptDTO.getDeptId());
                        customerCollaboration.setDepartmentName(userDeptDTO.getDeptName());
                    }
                    customerCollaboration.setCreateUserName(userNameMap.get(customerCollaboration.getCreateUser()));
                    customerCollaboration.setUpdateUserName(userNameMap.get(customerCollaboration.getUpdateUser()));
                    customerCollaboration.setUserName(userNameMap.get(customerCollaboration.getUserId()));
                    return customerCollaboration;
                }).toList();
    }

    public CustomerCollaboration add(CustomerCollaborationAddRequest request, String userId, String orgId) {
        CustomerCollaboration customerCollaboration = BeanUtils.copyBean(new CustomerCollaboration(), request);
        customerCollaboration.setCreateTime(System.currentTimeMillis());
        customerCollaboration.setUpdateTime(System.currentTimeMillis());
        customerCollaboration.setUpdateUser(userId);
        customerCollaboration.setCreateUser(userId);
        customerCollaboration.setId(IDGenerator.nextStr());
        customerCollaborationMapper.insert(customerCollaboration);

        // 添加协作人通知
        Customer customer = customerMapper.selectByPrimaryKey(request.getCustomerId());
        if (StringUtils.isNotBlank(customer.getOwner()) && !customer.getInSharedPool()) {
            // 公海客户添加协作人无需通知
            Map<String, String> userNameMap = baseService.getUserNameMap(List.of(userId, request.getUserId()));
            var paramMap = new HashMap<String, Object>(4);
            paramMap.put("useTemplate", "true");
            paramMap.put("template", Translator.get("message.customer.collaboration.add.text"));
            paramMap.put("operator", userNameMap.getOrDefault(userId, userId));
            paramMap.put("uName", userNameMap.getOrDefault(request.getUserId(), request.getUserId()));
            paramMap.put("name", customer.getName());
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                    NotificationConstants.Event.CUSTOMER_COLLABORATION_ADD,
                    paramMap, userId, orgId, List.of(customer.getOwner()), true);
        }
        return customerCollaborationMapper.selectByPrimaryKey(customerCollaboration.getId());
    }

    public CustomerCollaboration update(CustomerCollaborationUpdateRequest request, String userId) {
        associationPermissionService.checkCollaborations(List.of(request.getId()));
        extCustomerCollaborationMapper.updatePermission(request.getId(), request.getCollaborationType(),
                request.getExpireTime(), userId, System.currentTimeMillis());
        return customerCollaborationMapper.selectByPrimaryKey(request.getId());
    }

    public void delete(String id) {
        associationPermissionService.checkCollaborations(List.of(id));
        customerCollaborationMapper.deleteByPrimaryKey(id);
    }

    public void batchDelete(List<String> ids) {
        associationPermissionService.checkCollaborations(ids);
        customerCollaborationMapper.deleteByIds(ids);
    }

    public void deleteByCustomerIds(List<String> customerIds) {
        var wrapper = new LambdaQueryWrapper<CustomerCollaboration>();
        wrapper.in(CustomerCollaboration::getCustomerId, customerIds);
        customerCollaborationMapper.deleteByLambda(wrapper);
    }

    /**
     * 是否有客户协作关系
     *
     * @param userId     协作人
     * @param customerId 客户ID
     * @return bool
     */
    public boolean hasCollaboration(String userId, String customerId) {
        CustomerCollaboration example = new CustomerCollaboration();
        example.setUserId(userId);
        example.setCustomerId(customerId);
        return !customerCollaborationMapper.select(example).isEmpty();
    }

    /**
     * 获取客户所有协作人id
     */
    public Set<String> getCollaborations(String customerId) {
        CustomerCollaboration example = new CustomerCollaboration();
        example.setCustomerId(customerId);
        List<CustomerCollaboration> collaborations = customerCollaborationMapper.select(example);
        return collaborations.stream().map(CustomerCollaboration::getUserId).collect(Collectors.toSet());
    }
}
