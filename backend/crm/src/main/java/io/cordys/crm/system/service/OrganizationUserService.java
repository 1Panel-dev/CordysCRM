package io.cordys.crm.system.service;

import com.alibaba.excel.EasyExcelFactory;
import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.permission.PermissionCache;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.*;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.domain.*;
import io.cordys.crm.system.dto.convert.UserRoleConvert;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.UserImportDTO;
import io.cordys.crm.system.dto.response.UserImportResponse;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.excel.domain.UserExcelData;
import io.cordys.crm.system.excel.domain.UserExcelDataFactory;
import io.cordys.crm.system.excel.handler.UserTemplateWriteHandler;
import io.cordys.crm.system.excel.listener.UserCheckEventListener;
import io.cordys.crm.system.excel.listener.UserImportEventListener;
import io.cordys.crm.system.mapper.*;
import io.cordys.excel.utils.EasyExcelExporter;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import io.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service("organizationUserService")
@Transactional(rollbackFor = Exception.class)
public class OrganizationUserService {

    private static final String DEFAULT_USER_PASSWORD = "CordysCRM";

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
    @Resource
    private BaseMapper<Department> departmentMapper;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private PermissionCache permissionCache;

    /**
     * 员工列表查询
     *
     * @param request
     * @return
     */
    public List<UserPageResponse> list(UserPageRequest request) {
        List<UserPageResponse> list = extOrganizationUserMapper.list(request);
        handleData(list, request.getDepartmentIds().getFirst());
        return list;
    }


    private void handleData(List<UserPageResponse> list, String departmentId) {
        if (CollectionUtils.isNotEmpty(list)) {
            String orgId = list.stream().
                    map(UserPageResponse::getOrganizationId)
                    .toList()
                    .getFirst();
            List<String> userIds = list.stream()
                    .map(UserPageResponse::getUserId)
                    .toList();
            //获取用户角色
            List<UserRoleConvert> userRoles = extUserMapper.getUserRole(userIds, orgId);
            userRoles.forEach(role -> role.setName(roleService.translateInternalRole(role.getName())));
            Map<String, List<UserRoleConvert>> userRoleMap = userRoles.stream()
                    .collect(Collectors.groupingBy(UserRoleConvert::getUserId));
            //创建人 更新人
            List<String> ids = new ArrayList<>();
            ids.addAll(list.stream().map(UserPageResponse::getCreateUser).toList());
            ids.addAll(list.stream().map(UserPageResponse::getUpdateUser).toList());
            List<OptionDTO> optionDTOS = extUserMapper.selectUserOptionByIds(ids);
            Map<String, String> userMap = optionDTOS
                    .stream()
                    .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            //直属上级
            List<String> supervisorIds = list.stream()
                    .map(UserPageResponse::getSupervisorId)
                    .distinct()
                    .toList();
            List<OptionDTO> supervisorDTOS = extUserMapper.selectUserOptionByIds(supervisorIds);
            Map<String, String> supervisorMap = supervisorDTOS.stream()
                    .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            //部门
            List<String> departmentIds = list.stream()
                    .map(UserPageResponse::getDepartmentId)
                    .distinct()
                    .toList();
            List<Department> departmentList = departmentMapper.selectByIds(departmentIds.toArray(new String[0]));
            Map<String, String> departmentMap = departmentList.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName));
            //部门负责人
            List<String> commanderIds = extDepartmentCommanderMapper.selectCommander(departmentId, list.getFirst().getOrganizationId());

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
                if (departmentMap.containsKey(user.getDepartmentId())) {
                    user.setDepartmentName(departmentMap.get(user.getDepartmentId()));
                }
                if (supervisorMap.containsKey(user.getSupervisorId())) {
                    user.setSupervisorName(supervisorMap.get(user.getSupervisorId()));
                }
                if (commanderIds.contains(user.getUserId())) {
                    user.setCommander(true);
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
    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.ADD, operator = "{#operatorId}")
    public void addUser(UserAddRequest request, String organizationId, String operatorId) {
        String id = IDGenerator.nextStr();
        //邮箱和手机号唯一性校验
        checkEmailAndPhone(request.getEmail(), request.getPhone(), id);
        //add user base
        User user = addUserBaseData(request, operatorId, id);
        user.setLastOrganizationId(organizationId);
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
    private void checkEmailAndPhone(String email, String phone, String id) {
        if (extUserMapper.countByEmail(email, id) > 0) {
            throw new GenericException(Translator.get("email.exist"));
        }
        if (extUserMapper.countByPhone(phone, id) > 0) {
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
    private User addUserBaseData(UserAddRequest request, String operatorId, String id) {
        User user = new User();
        BeanUtils.copyBean(user, request);
        user.setId(id);
        user.setPassword(CodingUtils.md5(request.getPhone().substring(request.getPhone().length() - 6)));
        user.setCreateTime(System.currentTimeMillis());
        user.setCreateUser(operatorId);
        user.setUpdateTime(System.currentTimeMillis());
        user.setUpdateUser(operatorId);
        user.setLanguage(Locale.SIMPLIFIED_CHINESE.toString());
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
        if (StringUtils.isNotBlank(userDetail.getSupervisorId())) {
            User user = userMapper.selectByPrimaryKey(userDetail.getSupervisorId());
            userDetail.setSupervisorName(Optional.ofNullable(user).map(User::getName).orElse(null));
        }
        //获取用户角色
        List<UserRoleConvert> userRoles = extUserMapper.getUserRole(List.of(userDetail.getUserId()), userDetail.getOrganizationId());
        userRoles.forEach(role -> role.setName(roleService.translateInternalRole(role.getName())));
        userDetail.setRoles(userRoles);
        return userDetail;
    }

    /**
     * 更新用户
     *
     * @param request
     * @param operatorId
     */
    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.UPDATE, operator = "{#operatorId}")
    public void updateUser(UserUpdateRequest request, String operatorId, String orgId) {
        UserResponse oldUser = getUserDetail(request.getId());
        //邮箱和手机号唯一性校验
        checkEmailAndPhone(request.getEmail(), request.getPhone(), oldUser.getUserId());
        //update user info
        updateUserInfo(request, operatorId, oldUser);
        //update user base
        updateUserBaseData(request, operatorId, oldUser.getUserId());
        //update user role
        updateUserRole(request.getRoleIds(), oldUser, operatorId, orgId);
        if (!request.getEnable()) {
            SessionUtils.kickOutUser(oldUser.getUserId());
        }

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
    private void updateUserRole(List<String> roleIds, UserResponse oldUser, String operatorId, String orgId) {
        Optional.ofNullable(roleIds).ifPresent(ids -> {
            extUserRoleMapper.deleteUserRoleByUserId(oldUser.getUserId());
        });
        if (CollectionUtils.isNotEmpty(roleIds)) {
            addUserRole(roleIds, oldUser.getUserId(), operatorId);
        }
        permissionCache.clearCache(oldUser.getUserId(), orgId);
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
        User updateUser = BeanUtils.copyBean(new User(), request);
        updateUser.setId(userId);
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
    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.UPDATE, resourceId = "{#userId}", operator = "{#operatorId}")
    public void resetPassword(String userId, String operatorId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (StringUtils.isBlank(user.getPhone())) {
            throw new GenericException(Translator.get("user_phone_not_exist"));
        }

        user.setPassword(
                userId.equals(InternalUser.ADMIN.getValue())
                        ? CodingUtils.md5(DEFAULT_USER_PASSWORD)
                        : CodingUtils.md5(user.getPhone().substring(user.getPhone().length() - 6))
        );
        user.setUpdateTime(System.currentTimeMillis());
        user.setUpdateUser(operatorId);
        userMapper.updateById(user);

        //重置后被重置用户需要登出
        SessionUtils.kickOutUser(userId);

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
                LogDTO logDTO = new LogDTO(orgId, orgUser.getId(), operatorId, LogType.UPDATE, LogModule.SYSTEM_ORGANIZATION, orgUser.getName());
                logDTO.setOriginalValue(originUser);
                logDTO.setModifiedValue(newUser);
                logs.add(logDTO);
            });

            logService.batchAdd(logs);
        });

        // 踢出用户
        if (!request.isEnable()) {
            request.getIds().forEach(SessionUtils::kickOutUser);
        }
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
        if (CollectionUtils.isEmpty(userList)) {
            throw new GenericException(Translator.get("user_phone_not_exist"));
        }

        List<LogDTO> logDTOS = new ArrayList<>();
        User originPasswdUser = new User();
        originPasswdUser.setPassword("############");
        User newPasswdUser = new User();
        newPasswdUser.setPassword("************");
        userList.forEach(user -> {
            user.setPassword(
                    user.getId().equals(InternalUser.ADMIN.getValue())
                            ? CodingUtils.md5(DEFAULT_USER_PASSWORD)
                            : CodingUtils.md5(user.getPhone().substring(user.getPhone().length() - 6))
            );
            user.setUpdateTime(System.currentTimeMillis());
            user.setUpdateUser(operatorId);
            LogDTO logDTO = new LogDTO(orgId, user.getId(), operatorId, LogType.UPDATE, LogModule.SYSTEM_ORGANIZATION, user.getName());
            logDTO.setOriginalValue(originPasswdUser);
            logDTO.setModifiedValue(newPasswdUser);
            logDTOS.add(logDTO);
            //重置后被重置用户需要登出
            SessionUtils.kickOutUser(user.getId());
        });
        extUserMapper.batchUpdatePassword(userList);
        logService.batchAdd(logDTOS);

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
     * 更新同步信息
     *
     * @param updateUsers
     * @param updateOrganizationUsers
     */
    public void update(List<User> updateUsers, List<OrganizationUser> updateOrganizationUsers) {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtUserMapper extUserMapper = sqlSession.getMapper(ExtUserMapper.class);
        ExtOrganizationUserMapper extOrganizationUserMapper = sqlSession.getMapper(ExtOrganizationUserMapper.class);
        updateUsers.forEach(extUserMapper::updateUser);
        updateOrganizationUsers.forEach(extOrganizationUserMapper::updateOrganizationUser);
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }


    /**
     * 批量编辑用户
     *
     * @param request
     * @param operatorId
     * @param organizationId
     */
    public void batchEditUser(UserBatchEditRequest request, String operatorId, String organizationId) {
        List<UserResponse> originUser = extOrganizationUserMapper.selectByIds(request.getIds());
        extOrganizationUserMapper.updateUserByIds(request, operatorId, organizationId);
        List<UserResponse> modifiedUser = extOrganizationUserMapper.selectByIds(request.getIds());
        Map<String, UserResponse> userMap = modifiedUser.stream().collect(Collectors.toMap(UserResponse::getUserId, Function.identity()));


        List<LogDTO> logs = originUser.stream()
                .map(origUser -> {
                    LogDTO logDTO = new LogDTO(organizationId, origUser.getUserId(), operatorId, LogType.UPDATE, LogModule.SYSTEM_ORGANIZATION, origUser.getUserName());
                    logDTO.setOriginalValue(origUser);
                    logDTO.setModifiedValue(userMap.get(origUser.getUserId()));
                    return logDTO;
                }).toList();
        logService.batchAdd(logs);

    }


    /**
     * excel导入-下载模板
     *
     * @param response
     */
    public void downloadExcelTemplate(HttpServletResponse response) {
        //获取表头字段
        List<List<String>> heads = getTemplateHead();
        UserExcelData userExcelData = new UserExcelDataFactory().getUserExcelDataLocal();

        //表头备注信息
        UserTemplateWriteHandler handler = new UserTemplateWriteHandler(heads);
        List<List<Object>> data = new ArrayList<>();

        new EasyExcelExporter(userExcelData.getClass())
                .exportByCustomWriteHandler(response, heads, data, Translator.get("user_import_template_name"),
                        Translator.get("user_import_template_sheet"), handler);
    }


    /**
     * 获取表头字段
     *
     * @return
     */
    private List<List<String>> getTemplateHead() {
        return new UserExcelDataFactory().getUserExcelDataLocal().getHead();
    }


    /**
     * 导入excel检查
     *
     * @param file
     * @return
     */
    public UserImportResponse preCheck(MultipartFile file, String orgId) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }

        UserImportResponse response = new UserImportResponse();
        checkImportExcel(response, file, orgId);
        return response;
    }


    private void checkImportExcel(UserImportResponse response, MultipartFile file, String orgId) {
        try {
            //根据本地语言环境选择用哪种数据对象进行存放读取的数据
            Class<?> clazz = new UserExcelDataFactory().getExcelDataByLocal();
            UserCheckEventListener eventListener = new UserCheckEventListener(clazz, orgId);
            EasyExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(3).sheet().doRead();
            response.setErrorMessages(eventListener.getErrList());
            response.setSuccessCount(eventListener.getList().size());
            response.setFailCount(eventListener.getErrList().size());
        } catch (Exception e) {
            LogUtils.error("checkImportExcel error", e);
            throw new GenericException(Translator.get("check_import_excel_error"));
        }
    }


    /**
     * 用户导入
     *
     * @param file
     * @param operatorId
     * @param orgId
     * @return
     */
    public UserImportResponse importByExcel(MultipartFile file, String operatorId, String orgId) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        try {
            UserImportResponse response = new UserImportResponse();
            //根据本地语言环境选择用哪种数据对象进行存放读取的数据
            Class<?> clazz = new UserExcelDataFactory().getExcelDataByLocal();
            UserImportEventListener eventListener = new UserImportEventListener(clazz, operatorId, orgId);
            EasyExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(3).sheet().doRead();
            response.setErrorMessages(eventListener.getErrList());
            response.setSuccessCount(eventListener.getSuccessCount());
            response.setFailCount(eventListener.getErrList().size());
            return response;
        } catch (Exception e) {
            LogUtils.error("checkImportExcel error", e);
            throw new GenericException(Translator.get("check_import_excel_error"));
        }
    }


    /**
     * 保存导入数据
     *
     * @param list
     * @param departmentTree
     * @param departmentMap
     * @param operatorId
     * @param orgId
     */
    @CacheEvict(value = "dept_tree_cache", key = "#orgId", beforeInvocation = true)
    public void saveImportData(List<UserExcelData> list, List<BaseTreeNode> departmentTree, Map<String, String> departmentMap, String operatorId, String orgId) {
        //部门
        List<String> departmentPath = list.stream().map(UserExcelData::getDepartment).toList();
        //构建部门树
        Map<String, String> departmentPathMap = departmentService.createDepartment(departmentPath, orgId, departmentTree, operatorId, departmentMap);
        List<String> nameList = list.stream().map(UserExcelData::getSupervisor).filter(StringUtils::isNotBlank).toList();
        List<UserImportDTO> supervisorList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nameList)) {
            supervisorList = extOrganizationUserMapper.selectSupervisor(nameList, orgId);
        }

        List<User> userList = new ArrayList<>();
        List<OrganizationUser> organizationUsers = new ArrayList<>();
        List<UserImportDTO> finalSupervisorList = supervisorList;
        list.forEach(userData -> {
            buildUser(userData, operatorId, userList, departmentPathMap, orgId, organizationUsers, finalSupervisorList);
        });

        userMapper.batchInsert(userList);
        organizationUserMapper.batchInsert(organizationUsers);

    }

    private void buildUser(UserExcelData userData, String operatorId, List<User> userList, Map<String, String> departmentPathMap, String orgId, List<OrganizationUser> organizationUsers, List<UserImportDTO> supervisorList) {
        User user = new User();
        user.setId(IDGenerator.nextStr());
        user.setLastOrganizationId(orgId);
        user.setName(userData.getName());
        user.setPhone(userData.getPhone());
        user.setEmail(userData.getEmail());
        user.setPassword(CodingUtils.md5(userData.getPhone().substring(userData.getPhone().length() - 6)));
        user.setLanguage(Locale.SIMPLIFIED_CHINESE.toString());
        user.setGender(Boolean.valueOf(userData.getGender()));
        user.setCreateUser(operatorId);
        user.setCreateTime(System.currentTimeMillis());
        user.setUpdateUser(operatorId);
        user.setUpdateTime(System.currentTimeMillis());
        userList.add(user);

        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setId(IDGenerator.nextStr());
        organizationUser.setOrganizationId(orgId);
        organizationUser.setDepartmentId(departmentPathMap.get("/" + userData.getDepartment()));
        organizationUser.setUserId(user.getId());
        organizationUser.setEnable(true);
        organizationUser.setEmployeeId(userData.getEmployeeId());
        organizationUser.setPosition(userData.getPosition());
        organizationUser.setEmployeeType(userData.getEmployeeType());
        organizationUser.setSupervisorId(handleSupervisor(supervisorList, userData.getDepartment(), userData.getSupervisor()));
        organizationUser.setWorkCity(userData.getWorkCity());
        organizationUser.setCreateTime(System.currentTimeMillis());
        organizationUser.setCreateUser(operatorId);
        organizationUser.setUpdateTime(System.currentTimeMillis());
        organizationUser.setUpdateUser(operatorId);
        organizationUsers.add(organizationUser);

    }


    /**
     * 所属上级
     *
     * @param supervisorList
     * @param departmentId
     * @param name
     * @return
     */
    private String handleSupervisor(List<UserImportDTO> supervisorList, String departmentId, String name) {
        List<UserImportDTO> list = supervisorList.stream().filter(supervisor -> StringUtils.equalsIgnoreCase(supervisor.getName(), name) && StringUtils.equalsIgnoreCase(supervisor.getDepartmentId(), departmentId)).toList();
        if (CollectionUtils.isNotEmpty(list)) {
            return list.getFirst().getUserId();
        }
        if (CollectionUtils.isNotEmpty(supervisorList)) {
            return supervisorList.getFirst().getUserId();
        }
        return null;
    }


    /**
     * 导入校验电话号码唯一
     *
     * @param phone
     * @return
     */
    public boolean checkPhone(String phone) {
        return extUserMapper.countByPhone(phone, null) > 0;
    }

    /**
     * 导入校验邮箱唯一
     *
     * @param email
     * @return
     */
    public boolean checkEmail(String email) {
        return extUserMapper.countByEmail(email, null) > 0;
    }

    /**
     * 获取系统用户options
     *
     * @param orgId
     * @return
     */
    public List<OptionDTO> getUserOptions(String orgId) {
        return extUserMapper.selectUserOptionByOrgId(orgId);
    }


    /**
     * 删除用户（单个）
     *
     * @param id
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.DELETE, resourceId = "{#id}")
    public void deleteUserById(String id, String orgId) {
        UserResponse user = extUserMapper.getUserDetail(id);
        if (checkUserResource(user.getUserId())) {
            //删除后该员工在系统上的全部数据将会被清理
            deleteUserAllData(user.getUserId(), id, orgId);
            SessionUtils.kickOutUser(user.getUserId());
            // 添加日志上下文
            OperationLogContext.setResourceName(user.getUserName());
        } else {
            throw new GenericException(Translator.get("user_resource_exist"));
        }

        permissionCache.clearCache(user.getUserId(), orgId);
    }


    /**
     * 删除用户全部数据
     *
     * @param userId
     */
    private void deleteUserAllData(String userId, String id, String orgId) {
        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setUserId(userId);
        if (organizationUserMapper.countByExample(organizationUser) == 1) {
            //删除主用户
            userMapper.deleteByPrimaryKey(userId);
        }
        //删除用户
        organizationUserMapper.deleteByPrimaryKey(id);
        //删除用户关联角色
        List<String> userRoleIds = extUserRoleMapper.selectUserRole(userId, orgId);
        if (CollectionUtils.isNotEmpty(userRoleIds)) {
            extUserRoleMapper.deleteByIds(userRoleIds);
        }

    }

    public boolean checkUserResource(String id) {
        if (extOpportunityMapper.countByOwner(id) > 0 ||
                extCustomerMapper.countByOwner(id) > 0 ||
                extClueMapper.countByOwner(id) > 0) {
            return false;
        }

        return true;
    }

    public boolean deleteCheck(String id) {
        UserResponse user = extUserMapper.getUserDetail(id);
        return checkUserResource(user.getUserId());
    }

    public List<OrganizationUser> getUserByOrgId(String orgId) {
        return extOrganizationUserMapper.getUserByOrgId(orgId);
    }

    public void deleteUsers(List<OrganizationUser> userList) {
        if (CollectionUtils.isNotEmpty(userList)) {
            List<String> userIds = userList.stream()
                    .map(OrganizationUser::getUserId)
                    .toList();
            List<String> ids = userList.stream()
                    .map(OrganizationUser::getId)
                    .toList();
            LambdaQueryWrapper<OrganizationUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(OrganizationUser::getUserId, userIds);
            List<OrganizationUser> organizationUserList = organizationUserMapper.selectListByLambda(wrapper);
            Map<String, Long> userCountMap = organizationUserList.stream()
                    .collect(Collectors.groupingBy(OrganizationUser::getUserId, Collectors.counting()));

            List<String> singleUserIds = userCountMap.entrySet().stream()
                    .filter(entry -> entry.getValue() == 1)
                    .map(Map.Entry::getKey)
                    .toList();

            extOrganizationUserMapper.deleteUserByIds(ids);

            if (CollectionUtils.isNotEmpty(singleUserIds)) {
                this.extUserMapper.deleteByIds(userIds);
                this.extUserExtendMapper.deleteUser(userIds);
            }

        }
    }

    public String getSupervisorName(String id) {
        User user = userMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(user).map(User::getName).orElse(null);
    }


    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.UPDATE, operator = "{#operatorId}")
    public void updateUserName(UserUpdateName request, String operatorId) {
        User originUser = userMapper.selectByPrimaryKey(request.getUserId());

        User user = new User();
        user.setName(request.getName());
        user.setId(request.getUserId());
        userMapper.update(user);

        Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("name", originUser.getName());
        Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("name", request.getName());
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(originalVal)
                .resourceName(request.getName())
                .modifiedValue(modifiedVal)
                .resourceId(request.getUserId())
                .build());

    }
}