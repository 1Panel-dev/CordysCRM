package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.CustomerCapacity;
import cn.cordys.crm.customer.dto.CustomerCapacityDTO;
import cn.cordys.crm.customer.mapper.ExtCustomerCapacityMapper;
import cn.cordys.crm.system.dto.FilterConditionDTO;
import cn.cordys.crm.system.dto.request.CapacityAddRequest;
import cn.cordys.crm.system.dto.request.CapacityUpdateRequest;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerCapacityService {

  @Resource private UserExtendService userExtendService;
  @Resource private BaseMapper<CustomerCapacity> customerCapacityMapper;
  @Resource private ExtCustomerCapacityMapper extCustomerCapacityMapper;

  /**
   * 获取客户库容设置
   *
   * @return 客户库容设置集合
   */
  public List<CustomerCapacityDTO> list(String currentOrgId) {
    List<CustomerCapacityDTO> capacityList = new ArrayList<>();
    LambdaQueryWrapper<CustomerCapacity> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(CustomerCapacity::getOrganizationId, currentOrgId)
        .orderByDesc(CustomerCapacity::getCreateTime);
    List<CustomerCapacity> capacities = customerCapacityMapper.selectListByLambda(wrapper);
    if (CollectionUtils.isEmpty(capacities)) {
      return new ArrayList<>();
    }
    capacities.forEach(
        capacity -> {
          CustomerCapacityDTO capacityDTO = new CustomerCapacityDTO();
          capacityDTO.setId(capacity.getId());
          capacityDTO.setCapacity(capacity.getCapacity());
          capacityDTO.setMembers(
              userExtendService.getScope(JSON.parseArray(capacity.getScopeId(), String.class)));
          capacityDTO.setFilters(
              StringUtils.isEmpty(capacity.getFilter())
                  ? new ArrayList<>()
                  : JSON.parseArray(capacity.getFilter(), FilterConditionDTO.class));
          capacityList.add(capacityDTO);
        });
    return capacityList;
  }

  @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
  public void add(CapacityAddRequest request, String currentUserId, String currentOrgId) {
    List<CustomerCapacity> oldCapacities = customerCapacityMapper.selectAll(null);
    List<String> targetScopeIds =
        oldCapacities.stream()
            .flatMap(capacity -> JSON.parseArray(capacity.getScopeId(), String.class).stream())
            .collect(Collectors.toList());
    boolean duplicate =
        userExtendService.hasDuplicateScopeObj(request.getScopeIds(), targetScopeIds, currentOrgId);
    if (duplicate) {
      throw new GenericException(Translator.get("capacity.scope.duplicate"));
    }

    CustomerCapacity capacity = new CustomerCapacity();
    capacity.setId(IDGenerator.nextStr());
    capacity.setOrganizationId(currentOrgId);
    capacity.setCapacity(request.getCapacity());
    capacity.setScopeId(JSON.toJSONString(request.getScopeIds()));
    capacity.setFilter(
        CollectionUtils.isNotEmpty(request.getFilters())
            ? JSON.toJSONString(request.getFilters())
            : null);
    capacity.setCreateTime(System.currentTimeMillis());
    capacity.setCreateUser(currentUserId);
    capacity.setUpdateTime(System.currentTimeMillis());
    capacity.setUpdateUser(currentUserId);
    customerCapacityMapper.insert(capacity);

    // 添加日志上下文
    OperationLogContext.setContext(
        LogContextInfo.builder()
            .modifiedValue(capacity)
            .resourceId(capacity.getId())
            .resourceName(Translator.get("module.customer.capacity.setting"))
            .build());
  }

  @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
  public void update(CapacityUpdateRequest request, String currentUserId, String currentOrgId) {
    CustomerCapacity oldCapacity = customerCapacityMapper.selectByPrimaryKey(request.getId());
    if (oldCapacity == null) {
      throw new GenericException(Translator.get("capacity.not.exist"));
    }
    LambdaQueryWrapper<CustomerCapacity> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(CustomerCapacity::getOrganizationId, currentOrgId)
        .nq(CustomerCapacity::getId, request.getId());
    List<CustomerCapacity> oldCapacities = customerCapacityMapper.selectListByLambda(wrapper);
    List<String> targetScopeIds =
        oldCapacities.stream()
            .flatMap(capacity -> JSON.parseArray(capacity.getScopeId(), String.class).stream())
            .collect(Collectors.toList());
    boolean duplicate =
        userExtendService.hasDuplicateScopeObj(request.getScopeIds(), targetScopeIds, currentOrgId);
    if (duplicate) {
      throw new GenericException(Translator.get("capacity.scope.duplicate"));
    }
    oldCapacity.setScopeId(JSON.toJSONString(request.getScopeIds()));
    oldCapacity.setCapacity(request.getCapacity());
    oldCapacity.setFilter(
        CollectionUtils.isNotEmpty(request.getFilters())
            ? JSON.toJSONString(request.getFilters())
            : null);
    oldCapacity.setUpdateTime(System.currentTimeMillis());
    oldCapacity.setUpdateUser(currentUserId);
    extCustomerCapacityMapper.updateCapacity(oldCapacity);

    OperationLogContext.setContext(
        LogContextInfo.builder()
            .resourceId(request.getId())
            .resourceName(Translator.get("module.customer.capacity.setting"))
            .originalValue(oldCapacity)
            .modifiedValue(customerCapacityMapper.selectByPrimaryKey(request.getId()))
            .build());
  }

  @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
  public void delete(String id) {
    customerCapacityMapper.deleteByPrimaryKey(id);
    // 设置操作对象
    OperationLogContext.setResourceName(Translator.get("module.customer.capacity.setting"));
  }
}
