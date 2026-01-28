package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.common.util.*;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.UserViewConditionValueType;
import cn.cordys.crm.system.domain.UserView;
import cn.cordys.crm.system.domain.UserViewCondition;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.UserViewAddRequest;
import cn.cordys.crm.system.dto.request.UserViewUpdateRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.UserViewListResponse;
import cn.cordys.crm.system.dto.response.UserViewResponse;
import cn.cordys.crm.system.mapper.ExtUserViewConditionMapper;
import cn.cordys.crm.system.mapper.ExtUserViewMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userViewService")
@Transactional(rollbackFor = Exception.class)
public class UserViewService {

  @Resource private BaseMapper<UserView> userViewMapper;
  @Resource private ExtUserViewMapper extUserViewMapper;
  @Resource private BaseMapper<UserViewCondition> userViewConditionMapper;
  @Resource private ExtUserViewConditionMapper extUserViewConditionMapper;
  @Resource private ModuleFormCacheService moduleFormCacheService;
  @Resource private ModuleFormService moduleFormService;

  /**
   * 添加用户视图
   *
   * @param request 视图请求参数
   * @param userId 用户ID
   * @param orgId 组织ID
   * @param resourceType 视图类型
   * @return 新增的用户视图
   */
  public UserView add(
      UserViewAddRequest request, String userId, String orgId, String resourceType) {
    Long nextPos = getNextPos(orgId, userId, resourceType);
    String viewId = IDGenerator.nextStr();
    UserView userView = new UserView();
    userView.setId(viewId);
    userView.setName(request.getName());
    userView.setUserId(userId);
    userView.setResourceType(resourceType);
    userView.setFixed(false);
    userView.setEnable(true);
    userView.setOrganizationId(orgId);
    userView.setPos(nextPos);
    userView.setSearchMode(request.getSearchMode());
    userView.setCreateUser(userId);
    userView.setUpdateUser(userId);
    userView.setCreateTime(System.currentTimeMillis());
    userView.setUpdateTime(System.currentTimeMillis());
    userViewMapper.insert(userView);

    addUserViewConditions(request.getConditions(), viewId, userId);

    return userView;
  }

  private Long getNextPos(String orgId, String userId, String resourceType) {
    Long pos = extUserViewMapper.getNextPos(orgId, userId, resourceType);
    return (pos == null ? 0 : pos) + NodeSortUtils.DEFAULT_NODE_INTERVAL_POS;
  }

  /**
   * 解析自定义字段
   *
   * @param conditions
   * @param viewId
   * @param userId
   */
  private void addUserViewConditions(
      List<FilterCondition> conditions, String viewId, String userId) {
    if (CollectionUtils.isEmpty(conditions)) {
      return;
    }
    List<UserViewCondition> insertConditions =
        conditions.stream()
            .map(
                condition -> {
                  UserViewCondition userViewCondition = new UserViewCondition();
                  userViewCondition.setId(IDGenerator.nextStr());
                  Object value = condition.getValue();
                  String conditionValueType = getConditionValueType(value);
                  userViewCondition.setValueType(conditionValueType);
                  if (condition.getValue() != null) {
                    if (value instanceof List<?>) {
                      userViewCondition.setValue(JSON.toJSONString(value));
                    } else {
                      userViewCondition.setValue(condition.getValue().toString());
                    }
                  }
                  if (CollectionUtils.isNotEmpty(condition.getContainChildIds())) {
                    userViewCondition.setChildrenValue(
                        JSON.toJSONString(condition.getContainChildIds()));
                  }
                  userViewCondition.setName(condition.getName());
                  userViewCondition.setType(condition.getType());
                  userViewCondition.setMultipleValue(condition.getMultipleValue());
                  userViewCondition.setOperator(condition.getOperator());
                  userViewCondition.setSysUserViewId(viewId);
                  userViewCondition.setCreateUser(userId);
                  userViewCondition.setUpdateUser(userId);
                  userViewCondition.setCreateTime(System.currentTimeMillis());
                  userViewCondition.setUpdateTime(System.currentTimeMillis());
                  return userViewCondition;
                })
            .toList();

    userViewConditionMapper.batchInsert(insertConditions);
  }

  private String getConditionValueType(Object value) {
    if (value instanceof List<?>) {
      return UserViewConditionValueType.ARRAY.name();
    } else if (value instanceof Integer || value instanceof Long) {
      return UserViewConditionValueType.INT.name();
    } else if (value instanceof Float || value instanceof Double) {
      return UserViewConditionValueType.FLOAT.name();
    } else if (value instanceof Boolean) {
      return UserViewConditionValueType.BOOLEAN.name();
    } else {
      return UserViewConditionValueType.STRING.name();
    }
  }

  /**
   * 编辑用户视图
   *
   * @param request
   * @param userId
   * @param orgId
   * @return
   */
  public UserView update(UserViewUpdateRequest request, String userId, String orgId) {
    checkView(userId, request.getId(), orgId);

    UserView userView = new UserView();
    userView.setId(request.getId());
    userView.setName(request.getName());
    userView.setSearchMode(request.getSearchMode());
    userView.setUpdateTime(System.currentTimeMillis());
    userViewMapper.update(userView);

    // 先删除
    deleteConditionsByViewId(request.getId());
    // 再新增
    addUserViewConditions(request.getConditions(), request.getId(), userId);

    return userView;
  }

  private void deleteConditionsByViewId(String viewId) {
    extUserViewConditionMapper.delete(viewId);
  }

  private void checkView(String userId, String id, String orgId) {
    if (extUserViewMapper.countUserView(userId, id, orgId) <= 0) {
      throw new GenericException(Translator.get("view_blank"));
    }
  }

  /**
   * 删除视图
   *
   * @param id
   * @param userId
   * @param orgId
   */
  public void delete(String id, String userId, String orgId) {
    checkView(userId, id, orgId);
    userViewMapper.deleteByPrimaryKey(id);
    deleteConditionsByViewId(id);
  }

  /**
   * 获取视图详情
   *
   * @param id
   * @param userId
   * @param orgId
   * @return
   */
  public UserViewResponse getViewDetail(String id, String userId, String orgId, String formKey) {
    checkView(userId, id, orgId);
    UserView userView = userViewMapper.selectByPrimaryKey(id);
    List<FilterCondition> conditions = getFilterConditions(id);
    UserViewResponse response = new UserViewResponse();
    BeanUtils.copyBean(response, userView);
    response.setConditions(conditions);
    response.setOptionMap(buildOptionMap(orgId, formKey, conditions));
    return response;
  }

  public Map<String, List<OptionDTO>> buildOptionMap(
      String orgId, String formKey, List<FilterCondition> conditions) {
    Set<String> businessKeys =
        Arrays.stream(BusinessModuleField.values())
            .map(BusinessModuleField::getBusinessKey)
            .collect(Collectors.toSet());

    ModuleFormConfigDTO customerFormConfig =
        moduleFormCacheService.getBusinessFormConfig(formKey, orgId);

    Map<String, String> fieldIdBusinessKeyMap =
        customerFormConfig.getFields().stream()
            .filter(baseField -> businessKeys.contains(baseField.getBusinessKey()))
            .collect(Collectors.toMap(BaseField::getId, BaseField::getBusinessKey));

    // 获取数据源类型的字段值
    List<BaseModuleFieldValue> moduleFieldValues =
        conditions.stream()
            .filter(
                condition ->
                    Strings.CS.equalsAny(
                        condition.getType(),
                        FieldType.DATA_SOURCE.name(),
                        FieldType.DATA_SOURCE_MULTIPLE.name(),
                        FieldType.MEMBER.name(),
                        FieldType.MEMBER_MULTIPLE.name(),
                        FieldType.DEPARTMENT.name(),
                        FieldType.DEPARTMENT_MULTIPLE.name()))
            .map(
                condition -> {
                  BaseModuleFieldValue moduleFieldValue = new BaseModuleFieldValue();
                  moduleFieldValue.setFieldId(condition.getName());
                  moduleFieldValue.setFieldValue(condition.getValue());
                  return moduleFieldValue;
                })
            .collect(Collectors.toList());

    // 获取选项值对应的 option
    Map<String, List<OptionDTO>> optionMap =
        moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);
    fieldIdBusinessKeyMap.forEach(
        (fieldId, businessKey) -> {
          if (optionMap.containsKey(fieldId)) {
            List<OptionDTO> options = optionMap.get(fieldId);
            optionMap.put(businessKey, options);
            optionMap.remove(fieldId);
          }
        });
    return optionMap;
  }

  public List<FilterCondition> getFilterConditions(String viewId) {
    List<UserViewCondition> viewConditions = extUserViewConditionMapper.getViewConditions(viewId);
    return viewConditions.stream()
        .map(
            condition -> {
              FilterCondition filterCondition =
                  BeanUtils.copyBean(new FilterCondition(), condition);
              Object value =
                  getConditionValueByType(condition.getValueType(), condition.getValue());
              filterCondition.setValue(value);
              if (StringUtils.isNotBlank(condition.getChildrenValue())) {
                filterCondition.setContainChildIds(
                    JSON.parseArray(condition.getChildrenValue(), String.class));
              }
              return filterCondition;
            })
        .toList();
  }

  private Object getConditionValueByType(String valueType, String value) {
    UserViewConditionValueType conditionValueType =
        EnumUtils.valueOf(UserViewConditionValueType.class, valueType);
    if (StringUtils.isBlank(value)) {
      return null;
    }
    return switch (conditionValueType) {
      case ARRAY -> JSON.parseObject(value);
      case INT -> Long.valueOf(value);
      case FLOAT -> Double.valueOf(value);
      case BOOLEAN -> Boolean.valueOf(value);
      default -> value;
    };
  }

  /**
   * 视图列表
   *
   * @param resourceType
   * @param userId
   * @param orgId
   * @return
   */
  public List<UserViewListResponse> list(String resourceType, String userId, String orgId) {
    return extUserViewMapper.selectViewList(resourceType, userId, orgId);
  }

  /**
   * 固定/取消固定
   *
   * @param id
   * @param userId
   * @param orgId
   */
  public void fixed(String id, String userId, String orgId) {
    checkView(userId, id, orgId);
    UserView userView = userViewMapper.selectByPrimaryKey(id);
    UserView updateView = new UserView();
    updateView.setId(id);
    updateView.setFixed(!userView.getFixed());
    userViewMapper.update(updateView);
  }

  public void editPos(PosRequest request, String userId, String resourceType) {
    ServiceUtils.updatePosFieldByDesc(
        request,
        UserView.class,
        userId,
        resourceType,
        userViewMapper::selectByPrimaryKey,
        extUserViewMapper::getPrePos,
        extUserViewMapper::getLastPos,
        userViewMapper::update);
  }

  /**
   * 启用视图
   *
   * @param id
   * @param userId
   * @param orgId
   */
  public void enable(String id, String userId, String orgId) {
    checkView(userId, id, orgId);
    UserView userView = userViewMapper.selectByPrimaryKey(id);
    changeEnable(id, !userView.getEnable());
  }

  private void changeEnable(String id, boolean enable) {
    UserView userView = new UserView();
    userView.setId(id);
    userView.setEnable(enable);
    userViewMapper.update(userView);
  }

  public String getSearchMode(String viewId) {
    UserView userView = userViewMapper.selectByPrimaryKey(viewId);
    return Optional.ofNullable(userView)
        .map(UserView::getSearchMode)
        .orElse(CombineSearch.SearchMode.AND.name());
  }
}
