package cn.cordys.crm.customer.service;

import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerOwner;
import cn.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import cn.cordys.crm.customer.dto.response.CustomerOwnerListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerOwnerMapper;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.domain.DictConfig;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerOwnerHistoryService {
  @Resource private BaseMapper<CustomerOwner> customerOwnerMapper;
  @Resource private BaseService baseService;
  @Resource private ExtCustomerOwnerMapper extCustomerOwnerMapper;
  @Resource private BaseMapper<DictConfig> dictConfigMapper;
  @Resource private BaseMapper<Dict> dictMapper;

  public List<CustomerOwnerListResponse> list(String customerId, String orgId) {
    LambdaQueryWrapper<CustomerOwner> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(CustomerOwner::getCustomerId, customerId);
    wrapper.orderByDesc(CustomerOwner::getEndTime);
    List<CustomerOwner> owners = customerOwnerMapper.selectListByLambda(wrapper);
    return buildListData(orgId, owners);
  }

  private List<CustomerOwnerListResponse> buildListData(String orgId, List<CustomerOwner> owners) {
    if (CollectionUtils.isEmpty(owners)) {
      return List.of();
    }
    Set<String> userIds = new HashSet<>();
    Set<String> ownerIds = new HashSet<>();
    Set<String> reasonIds = new HashSet<>();

    for (CustomerOwner owner : owners) {
      userIds.add(owner.getOwner());
      userIds.add(owner.getOperator());
      ownerIds.add(owner.getOwner());
      reasonIds.add(owner.getReasonId());
    }

    Map<String, UserDeptDTO> userDeptMap =
        baseService.getUserDeptMapByUserIds(new ArrayList<>(ownerIds), orgId);

    Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

    List<Dict> dictList = dictMapper.selectByIds(new ArrayList<>(reasonIds));
    Map<String, String> dictNameMap =
        dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

    LambdaQueryWrapper<DictConfig> configLambdaQueryWrapper = new LambdaQueryWrapper<>();
    configLambdaQueryWrapper
        .eq(DictConfig::getModule, DictModule.CUSTOMER_POOL_RS.name())
        .eq(DictConfig::getOrganizationId, orgId);
    List<DictConfig> configs = dictConfigMapper.selectListByLambda(configLambdaQueryWrapper);

    boolean showReason = CollectionUtils.isNotEmpty(configs) && configs.getFirst().getEnabled();
    return owners.stream()
        .map(
            item -> {
              CustomerOwnerListResponse customerOwner =
                  BeanUtils.copyBean(new CustomerOwnerListResponse(), item);
              UserDeptDTO userDeptDTO = userDeptMap.get(customerOwner.getOwner());
              if (userDeptDTO != null) {
                customerOwner.setDepartmentId(userDeptDTO.getDeptId());
                customerOwner.setDepartmentName(userDeptDTO.getDeptName());
              }
              if (!showReason) {
                customerOwner.setReasonId(null);
                customerOwner.setReasonName(null);
              } else {
                customerOwner.setReasonName(dictNameMap.get(customerOwner.getReasonId()));
              }
              customerOwner.setOwnerName(userNameMap.get(customerOwner.getOwner()));
              customerOwner.setOperatorName(userNameMap.get(customerOwner.getOperator()));
              return customerOwner;
            })
        .toList();
  }

  public CustomerOwner add(Customer customer, String userId, Boolean addReason) {
    CustomerOwner customerOwner = new CustomerOwner();
    customerOwner.setOwner(customer.getOwner());
    customerOwner.setOperator(userId);
    customerOwner.setCustomerId(customer.getId());
    customerOwner.setCollectionTime(customer.getCollectionTime());
    customerOwner.setEndTime(System.currentTimeMillis());
    if (addReason
        && StringUtils.isNotBlank(customer.getReasonId())
        && !Strings.CS.equals(customer.getReasonId(), "system")) {
      customerOwner.setReasonId(customer.getReasonId());
    }
    customerOwner.setId(IDGenerator.nextStr());
    customerOwnerMapper.insert(customerOwner);
    return customerOwner;
  }

  public void batchAdd(CustomerBatchTransferRequest transferRequest, String userId) {
    extCustomerOwnerMapper.batchAdd(transferRequest, userId);
  }

  public void deleteByCustomerIds(List<String> customerIds) {
    LambdaQueryWrapper<CustomerOwner> wrapper = new LambdaQueryWrapper<>();
    wrapper.in(CustomerOwner::getCustomerId, customerIds);
    customerOwnerMapper.deleteByLambda(wrapper);
  }
}
