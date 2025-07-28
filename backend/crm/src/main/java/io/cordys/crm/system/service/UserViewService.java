package io.cordys.crm.system.service;

import io.cordys.common.dto.condition.FilterCondition;
import io.cordys.common.dto.request.PosRequest;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.uid.utils.EnumUtils;
import io.cordys.common.util.*;
import io.cordys.crm.system.constants.UserViewConditionValueType;
import io.cordys.crm.system.domain.UserView;
import io.cordys.crm.system.domain.UserViewCondition;
import io.cordys.crm.system.dto.request.UserViewAddRequest;
import io.cordys.crm.system.dto.request.UserViewUpdateRequest;
import io.cordys.crm.system.dto.response.UserViewListResponse;
import io.cordys.crm.system.dto.response.UserViewResponse;
import io.cordys.crm.system.mapper.ExtUserViewConditionMapper;
import io.cordys.crm.system.mapper.ExtUserViewMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("userViewService")
@Transactional(rollbackFor = Exception.class)
public class UserViewService {

    @Resource
    private BaseMapper<UserView> userViewMapper;
    @Resource
    private ExtUserViewMapper extUserViewMapper;
    @Resource
    private BaseMapper<UserViewCondition> userViewConditionMapper;
    @Resource
    private ExtUserViewConditionMapper extUserViewConditionMapper;

    /**
     * 添加用户视图
     *
     * @param request      视图请求参数
     * @param userId       用户ID
     * @param orgId        组织ID
     * @param resourceType 视图类型
     * @return 新增的用户视图
     */
    public UserView add(UserViewAddRequest request, String userId, String orgId, String resourceType) {
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
    private void addUserViewConditions(List<FilterCondition> conditions, String viewId, String userId) {
        if (CollectionUtils.isEmpty(conditions)) {
            return;
        }
        List<UserViewCondition> insertConditions = conditions.stream().map(condition -> {
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
        }).toList();

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
    public UserViewResponse getViewDetail(String id, String userId, String orgId) {
        checkView(userId, id, orgId);
        UserView userView = userViewMapper.selectByPrimaryKey(id);
        List<UserViewCondition> viewConditions = extUserViewConditionMapper.getViewConditions(id);
        List<FilterCondition> conditions = viewConditions.stream().map(condition -> {
            FilterCondition filterCondition = BeanUtils.copyBean(new FilterCondition(), condition);
            Object value = getConditionValueByType(condition.getValueType(), condition.getValue());
            filterCondition.setValue(value);
            return filterCondition;
        }).toList();
        UserViewResponse response = new UserViewResponse();
        BeanUtils.copyBean(response, userView);
        response.setConditions(conditions);
        return response;
    }


    private Object getConditionValueByType(String valueType, String value) {
        UserViewConditionValueType conditionValueType = EnumUtils.valueOf(UserViewConditionValueType.class, valueType);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        switch (conditionValueType) {
            case ARRAY:
                return JSON.parseObject(value);
            case INT:
                return Long.valueOf(value);
            case FLOAT:
                return Double.valueOf(value);
            case BOOLEAN:
                return Boolean.valueOf(value);
            default:
                return value;
        }
    }


    /**
     * 视图列表
     *
     * @param resourceType
     * @param userId
     * @param orgId
     * @return
     */
    public List<UserViewListResponse> list(String resourceType, String userId, String orgId, boolean isOption) {
        return extUserViewMapper.selectViewList(resourceType, userId, orgId, isOption);
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

    public void editPos(PosRequest request, String userId, String orgId) {
        ServiceUtils.updatePosField(request,
                UserView.class,
                userId,
                userViewMapper::selectByPrimaryKey,
                extUserViewMapper::getPrePos,
                extUserViewMapper::getLastPos,
                userViewMapper::update);
    }

}
