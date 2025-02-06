package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.SubListUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.*;
import io.cordys.crm.system.dto.convert.UserRoleConvert;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.mapper.*;
import io.cordys.integration.wecom.dto.WeComDepartment;
import io.cordys.integration.wecom.dto.WeComUser;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
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
    private RoleService roleService;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private BaseMapper<UserRole> userRoleMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private LogService logService;
    @Resource
    private ExtDepartmentCommanderMapper extDepartmentCommanderMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private BaseMapper<DepartmentCommander> departmentCommanderMapper;
    @Resource
    private BaseMapper<UserExtend> userExtendMapper;
    @Resource
    private ExtUserExtendMapper extUserExtendMapper;

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
            userRoles.forEach(role -> role.setName(roleService.translateInternalRole(role.getName())));
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
    @OperationLog(module = LogModule.SYSTEM_DEPARTMENT_USER, type = LogType.ADD, operator = "{#operatorId}")
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
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(null)
                .modifiedValue(user)
                .resourceName(user.getName())
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
        user.setPassword(CodingUtils.md5(request.getPhone().substring(request.getPhone().length() - 6)));
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
    @OperationLog(module = LogModule.SYSTEM_DEPARTMENT_USER, type = LogType.UPDATE, operator = "{#operatorId}")
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
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(oldUser)
                .resourceName(oldUser.getUserName())
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

    /**
     * 重置密码
     *
     * @param userId
     * @param operatorId
     */
    @OperationLog(module = LogModule.SYSTEM_DEPARTMENT_USER, type = LogType.UPDATE, resourceId = "{#userId}", operator = "{#operatorId}")
    public void resetPassword(String userId, String operatorId) {
        User user = userMapper.selectByPrimaryKey(userId);
        user.setPassword(CodingUtils.md5(user.getPhone().substring(user.getPhone().length() - 6)));
        user.setUpdateTime(System.currentTimeMillis());
        user.setUpdateUser(operatorId);
        userMapper.updateById(user);

        // 日志详情对比需要有差异，并且脱敏
        User originPasswdUser = new User();
        originPasswdUser.setPassword("############");
        User newPasswdUser = new User();
        newPasswdUser.setPassword("************");
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(originPasswdUser)
                .modifiedValue(newPasswdUser)
                .resourceName(user.getName())
                .build());
    }

    /**
     * 批量启用/禁用
     *
     * @param request
     * @param operatorId
     */
    public void enable(UserBatchEnableRequest request, String operatorId, String orgId) {
        extOrganizationUserMapper.enable(request, operatorId, System.currentTimeMillis());

        // 记录日志
        OrganizationUser originUser = new OrganizationUser();
        originUser.setEnable(!request.isEnable());
        OrganizationUser newUser = new OrganizationUser();
        newUser.setEnable(request.isEnable());
        SubListUtils.dealForSubList(request.getIds(), 50, ids -> {
            List<OptionDTO> orgUsers = extOrganizationUserMapper.selectEnableOrgUser(ids, !request.isEnable());
            List<LogDTO> logs = new ArrayList<>();
            orgUsers.forEach(orgUser -> {
                LogDTO logDTO = new LogDTO(orgId, orgUser.getId(), operatorId, LogType.UPDATE, LogModule.SYSTEM_DEPARTMENT_USER, orgUser.getName());
                logDTO.setOriginalValue(originUser);
                logDTO.setModifiedValue(newUser);
                logs.add(logDTO);
            });

            logService.batchAdd(logs);
        });
    }


    /**
     * 批量重置密码
     *
     * @param request
     * @param operatorId
     * @param orgId
     */
    public void batchResetPassword(UserBatchRequest request, String operatorId, String orgId) {
        List<User> userList = extOrganizationUserMapper.getUserList(request);

        List<LogDTO> logDTOS = new ArrayList<>();
        User originPasswdUser = new User();
        originPasswdUser.setPassword("############");
        User newPasswdUser = new User();
        newPasswdUser.setPassword("************");
        userList.forEach(user -> {
            user.setPassword(CodingUtils.md5(user.getPhone().substring(user.getPhone().length() - 6)));
            user.setUpdateTime(System.currentTimeMillis());
            user.setUpdateUser(operatorId);
            LogDTO logDTO = new LogDTO(orgId, user.getId(), operatorId, LogType.UPDATE, LogModule.SYSTEM_DEPARTMENT_USER, user.getName());
            logDTO.setOriginalValue(originPasswdUser);
            logDTO.setModifiedValue(newPasswdUser);
            logDTOS.add(logDTO);
        });
        extUserMapper.batchUpdatePassword(userList);
        logService.batchAdd(logDTOS);

    }

    /**
     * 同步组织架构
     *
     * @param type
     * @param operatorId
     * @param orgId
     */
    @Async
    public void syncUser(String type, String operatorId, String orgId) {
        SyncUserServiceFactory.getSyncUserService(type).syncUser(operatorId, orgId);
    }

    public void buildUser(WeComDepartment weComDepartment, List<WeComUser> weComUsers, String operatorId, String orgId,
                          List<User> users, List<UserExtend> userExtends, List<OrganizationUser> organizationUsers, List<DepartmentCommander> departmentCommanders) {
        if (CollectionUtils.isNotEmpty(weComUsers)) {
            weComUsers.forEach(weComUser -> {
                String id = IDGenerator.nextStr();
                //基本信息
                User user = new User();
                user.setId(id);
                user.setName(weComUser.getName());
                user.setPhone(weComUser.getMobile());
                user.setEmail(weComUser.getEmail());
                user.setPassword(CodingUtils.md5("CordysCRM"));
                if (weComUser.getGender() != null) {
                    user.setGender(weComUser.getGender() == 1 ? false : true);
                } else {
                    user.setGender(false);
                }
                user.setLanguage("zh_CN");
                user.setCreateTime(System.currentTimeMillis());
                user.setCreateUser(operatorId);
                user.setUpdateTime(System.currentTimeMillis());
                user.setUpdateUser(operatorId);
                users.add(user);

                //拓展信息
                UserExtend userExtend = new UserExtend();
                userExtend.setId(id);
                userExtend.setAvatar(weComUser.getAvatar());
                userExtends.add(userExtend);
                //其他信息
                OrganizationUser organizationUser = new OrganizationUser();
                organizationUser.setId(IDGenerator.nextStr());
                organizationUser.setDepartmentId(weComDepartment.getCrmId());
                organizationUser.setOrganizationId(orgId);
                organizationUser.setUserId(id);
                organizationUser.setResourceUserId(weComUser.getUserId());
                organizationUser.setEnable(true);
                organizationUser.setPosition(weComUser.getPosition());
                organizationUser.setCreateTime(System.currentTimeMillis());
                organizationUser.setCreateUser(operatorId);
                organizationUser.setUpdateTime(System.currentTimeMillis());
                organizationUser.setUpdateUser(operatorId);
                organizationUsers.add(organizationUser);


                int i = weComUser.getDepartment().indexOf(weComDepartment.getId());
                if (weComUser.getIsLeaderInDept().get(i) == 1) {
                    //构建部门责任人信息
                    DepartmentCommander departmentCommander = new DepartmentCommander();
                    departmentCommander.setId(IDGenerator.nextStr());
                    departmentCommander.setUserId(id);
                    departmentCommander.setDepartmentId(weComDepartment.getCrmId());
                    departmentCommander.setCreateTime(System.currentTimeMillis());
                    departmentCommander.setCreateUser(operatorId);
                    departmentCommander.setUpdateTime(System.currentTimeMillis());
                    departmentCommander.setUpdateUser(operatorId);
                    departmentCommanders.add(departmentCommander);
                }

            });
        }

    }


    /**
     * 删除用户
     *
     * @param orgId
     */
    public void deleteUser(String orgId) {
        List<Department> departmentList = extDepartmentMapper.selectAllDepartment(orgId);
        if (CollectionUtils.isNotEmpty(departmentList)) {
            extDepartmentCommanderMapper.deleteByDepartmentIds(departmentList.stream().map(Department::getId).toList());
        }
        List<User> userList = extUserMapper.getAllUserIds(orgId);
        List<String> ids = userList.stream().map(User::getId).toList();
        if (CollectionUtils.isNotEmpty(ids)) {
            extUserMapper.deleteByIds(ids);
            extUserExtendMapper.deleteUser(ids);
        }
        extOrganizationUserMapper.deleteUserByOrgId(orgId);

    }

    /**
     * 保存同步信息
     *
     * @param users
     * @param userExtends
     * @param organizationUsers
     * @param departmentCommanders
     */
    public void save(List<User> users, List<UserExtend> userExtends, List<OrganizationUser> organizationUsers, List<DepartmentCommander> departmentCommanders) {
        userMapper.batchInsert(users);
        userExtendMapper.batchInsert(userExtends);
        organizationUserMapper.batchInsert(organizationUsers);
        departmentCommanderMapper.batchInsert(departmentCommanders);
    }


    /**
     * 批量编辑用户
     *
     * @param request
     * @param operatorId
     * @param organizationId
     */
    public void batchEditUser(UserBatchEditRequest request, String operatorId, String organizationId) {
        extOrganizationUserMapper.updateUserByIds(request, operatorId, organizationId);
        //todo 日志
    }
}