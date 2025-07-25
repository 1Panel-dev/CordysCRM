package io.cordys.crm.system.service;

import io.cordys.common.dto.condition.FilterCondition;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.common.util.NodeSortUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.UserView;
import io.cordys.crm.system.domain.UserViewCondition;
import io.cordys.crm.system.dto.request.UserViewAddRequest;
import io.cordys.crm.system.dto.request.UserViewUpdateRequest;
import io.cordys.crm.system.mapper.ExtUserViewMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
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

    /**
     * 添加用户视图
     *
     * @param request 视图请求参数
     * @param userId  用户ID
     * @param orgId   组织ID
     * @return 新增的用户视图
     */
    public UserView add(UserViewAddRequest request, String userId, String orgId) {
        Long nextPos = getNextPos(orgId, userId, request.getResourceType());
        String viewId = IDGenerator.nextStr();
        UserView userView = new UserView();
        userView.setId(viewId);
        userView.setName(request.getName());
        userView.setUserId(userId);
        userView.setResourceType(request.getResourceType());
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

    private void deleteConditionsByViewId(String id) {
        LambdaQueryWrapper<UserViewCondition> viewWrapper = new LambdaQueryWrapper<>();
        viewWrapper.eq(UserViewCondition::getSysUserViewId, id);
        userViewConditionMapper.deleteByLambda(viewWrapper);
    }

    private void checkView(String userId, String id, String orgId) {
        if (extUserViewMapper.countUserView(userId, id, orgId) <= 0) {
            throw new GenericException(Translator.get("view_blank"));
        }
    }
}
