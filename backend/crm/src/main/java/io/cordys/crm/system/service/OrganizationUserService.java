package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogExtraDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.domain.UserRole;
import io.cordys.crm.system.dto.convert.UserRoleConvert;
import io.cordys.crm.system.dto.request.UserAddRequest;
import io.cordys.crm.system.dto.request.UserPageRequest;
import io.cordys.crm.system.dto.request.UserUpdateRequest;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.mapper.ExtUserRoleMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("organizationUserService")
@Transactional(rollbackFor = Exception.class)
public class OrganizationUserService {

    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private BaseMapper<UserRole> userRoleMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;

    /**
     * 员工列表查询
     *
     * @param request
     * @return
     */
    public List<UserPageResponse> list(UserPageRequest request) {
        List<UserPageResponse> list = extOrganizationUserMapper.list(request);
        handleData(list);
        return list;
    }


    private void handleData(List<UserPageResponse> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            String orgId = list.stream().map(UserPageResponse::getOrganizationId).toList().getFirst();
            List<String> userIds = list.stream().map(UserPageResponse::getUserId).toList();
            //获取用户角色
            List<UserRoleConvert> userRoles = extUserMapper.getUserRole(userIds, orgId);
            Map<String, List<UserRoleConvert>> userRoleMap = userRoles.stream().collect(Collectors.groupingBy(UserRoleConvert::getUserId));
            //创建人 更新人
            List<String> ids = new ArrayList<>();
            ids.addAll(list.stream().map(UserPageResponse::getCreateUser).toList());
            ids.addAll(list.stream().map(UserPageResponse::getUpdateUser).toList());
            List<OptionDTO> optionDTOS = extUserMapper.selectUserOptionByIds(ids);
            Map<String, String> userMap = optionDTOS.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

            //todo 暂无用户组 后续需求再处理
            list.forEach(user -> {
                if (userRoleMap.containsKey(user.getUserId())) {
                    user.setRoles(userRoleMap.get(user.getUserId()));
                }
                if (userMap.containsKey(user.getCreateUser())) {
                    user.setCreateUserName(userMap.get(user.getCreateUser()));
                }
                if (userMap.containsKey(user.getUpdateUser())) {
                    user.setUpdateUserName(userMap.get(user.getUpdateUser()));
                }
            });

        }
    }


    /**
     * 添加员工
     *
     * @param request
     * @param organizationId
     * @param operatorId
     */
    @OperationLog(module = LogModule.SYSTEM, type = LogType.ADD, operator = "{{#operatorId}}", success = "添加用户成功", extra = "{{#newUser}}")
    public void addUser(UserAddRequest request, String organizationId, String operatorId) {
        //邮箱和手机号唯一性校验
        checkEmailAndPhone(request.getEmail(), request.getPhone());
        //add user base
        User user = addUserBaseData(request, operatorId);
        //add user info
        addUserInfo(request, organizationId, operatorId, user.getId());
        //add user role
        addUserRole(request.getRoleIds(), user.getId(), operatorId);
        //todo add user group
        //添加日志上下文
        OperationLogContext.putVariable("newUser", LogExtraDTO.builder()
                .originalValue(null)
                .modifiedValue(user)
                .resourceId(user.getId())
                .build());
    }


    /**
     * 添加用户角色关系
     *
     * @param roleIds
     * @param userId
     * @param operatorId
     */
    private void addUserRole(List<String> roleIds, String userId, String operatorId) {
        if (CollectionUtils.isNotEmpty(roleIds)) {
            List<UserRole> list = new ArrayList<>();
            roleIds.forEach(roleId -> {
                UserRole userRole = new UserRole();
                userRole.setId(IDGenerator.nextStr());
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setCreateTime(System.currentTimeMillis());
                userRole.setCreateUser(operatorId);
                userRole.setUpdateTime(System.currentTimeMillis());
                userRole.setUpdateUser(operatorId);
                list.add(userRole);
            });
            userRoleMapper.batchInsert(list);
        }
    }


    /**
     * 添加用戶信息
     *
     * @param request
     * @param organizationId
     * @param userId
     */
    private void addUserInfo(UserAddRequest request, String organizationId, String operatorId, String userId) {
        OrganizationUser orgUser = new OrganizationUser();
        BeanUtils.copyBean(orgUser, request);
        orgUser.setId(IDGenerator.nextStr());
        orgUser.setOrganizationId(organizationId);
        orgUser.setUserId(userId);
        orgUser.setCreateTime(System.currentTimeMillis());
        orgUser.setCreateUser(operatorId);
        orgUser.setUpdateTime(System.currentTimeMillis());
        orgUser.setUpdateUser(operatorId);
        organizationUserMapper.insert(orgUser);
    }

    /**
     * 邮箱和手机号唯一性校验
     *
     * @param email
     * @param phone
     */
    private void checkEmailAndPhone(String email, String phone) {
        User user = new User();
        user.setEmail(email);
        if (userMapper.countByExample(user) > 0) {
            throw new GenericException(Translator.get("email.exist"));
        }
        user.setEmail(null);
        user.setPhone(phone);
        if (userMapper.countByExample(user) > 0) {
            throw new GenericException(Translator.get("phone.exist"));
        }

    }

    /**
     * 添加用户基本数据
     *
     * @param request
     * @param operatorId
     * @return
     */
    private User addUserBaseData(UserAddRequest request, String operatorId) {
        User user = new User();
        BeanUtils.copyBean(user, request);
        user.setId(IDGenerator.nextStr());
        user.setPassword(request.getPhone().substring(request.getPhone().length() - 6));
        user.setCreateTime(System.currentTimeMillis());
        user.setCreateUser(operatorId);
        user.setUpdateTime(System.currentTimeMillis());
        user.setUpdateUser(operatorId);
        userMapper.insert(user);
        return user;
    }


    /**
     * 获取用户详情
     *
     * @param id
     * @return
     */
    public UserResponse getUserDetail(String id) {
        UserResponse userDetail = extUserMapper.getUserDetail(id);
        //获取用户角色
        List<UserRoleConvert> userRoles = extUserMapper.getUserRole(List.of(userDetail.getUserId()), userDetail.getOrganizationId());
        userDetail.setRoles(userRoles);
        return userDetail;
    }

    /**
     * 更新用户
     *
     * @param request
     * @param operatorId
     */
    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, operator = "{{#operatorId}}", success = "更新用户成功", extra = "{{#newUser}}")
    public void updateUser(UserUpdateRequest request, String operatorId) {
        //邮箱和手机号唯一性校验
        checkEmailAndPhone(request.getEmail(), request.getPhone());
        UserResponse oldUser = getUserDetail(request.getId());
        //update user info
        updateUserInfo(request, operatorId, oldUser);
        //update user base
        updateUserBaseData(request, operatorId, oldUser.getUserId());
        //update user role
        updateUserRole(request.getRoleIds(), oldUser, operatorId);
        //todo update user group


        //添加日志上下文
        OperationLogContext.putVariable("newUser", LogExtraDTO.builder()
                .originalValue(oldUser)
                .modifiedValue(getUserDetail(request.getId()))
                .resourceId(oldUser.getId())
                .build());
    }

    /**
     * 更新用户角色
     *
     * @param roleIds
     * @param oldUser
     * @param operatorId
     */
    private void updateUserRole(List<String> roleIds, UserResponse oldUser, String operatorId) {
        List<String> ids = oldUser.getRoles().stream().map(UserRoleConvert::getId).toList();
        if (CollectionUtils.isNotEmpty(ids)) {
            extUserRoleMapper.deleteUserRole(ids);
        }
        if (CollectionUtils.isNotEmpty(roleIds)) {
            addUserRole(roleIds, oldUser.getUserId(), operatorId);
        }
    }

    /**
     * 更新用户信息
     *
     * @param request
     * @param operatorId
     * @return
     */
    private OrganizationUser updateUserInfo(UserUpdateRequest request, String operatorId, UserResponse user) {
        OrganizationUser organizationUser = BeanUtils.copyBean(new OrganizationUser(), user);
        BeanUtils.copyBean(organizationUser, request);
        organizationUser.setUpdateTime(System.currentTimeMillis());
        organizationUser.setUpdateUser(operatorId);
        organizationUserMapper.updateById(organizationUser);
        return organizationUser;
    }


    /**
     * 更新用户基本数据
     *
     * @param request
     * @param operatorId
     * @param userId
     */
    private void updateUserBaseData(UserUpdateRequest request, String operatorId, String userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        User updateUser = BeanUtils.copyBean(user, request);
        updateUser.setUpdateTime(System.currentTimeMillis());
        updateUser.setUpdateUser(operatorId);
        userMapper.updateById(updateUser);
    }
}