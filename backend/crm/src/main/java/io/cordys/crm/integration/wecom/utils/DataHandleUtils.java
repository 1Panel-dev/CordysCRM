package io.cordys.crm.integration.wecom.utils;

import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.NodeSortUtils;
import io.cordys.crm.integration.wecom.dto.WeComDepartment;
import io.cordys.crm.integration.wecom.dto.WeComUser;
import io.cordys.crm.system.domain.*;
import io.cordys.crm.system.service.DepartmentService;
import io.cordys.crm.system.service.OrganizationConfigService;
import io.cordys.crm.system.service.OrganizationUserService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DataHandleUtils {

    private final String orgId;
    private final DepartmentService departmentService;
    private final OrganizationUserService organizationUserService;
    private final OrganizationConfigService organizationConfigService;
    private final List<Department> addDepartments = new ArrayList<>();
    private final List<Department> updateDepartments = new ArrayList<>();
    private List<WeComDepartment> weComDepartmentTree;
    private final List<User> addUsers = new ArrayList<>();
    private final List<User> updateUsers = new ArrayList<>();
    private final List<UserExtend> userExtends = new ArrayList<>();
    private final List<OrganizationUser> organizationUsers = new ArrayList<>();
    private final List<OrganizationUser> updateOrganizationUsers = new ArrayList<>();
    private final Map<String, List<WeComUser>> departmentUserMap;
    private final List<DepartmentCommander> departmentCommanders = new ArrayList<>();
    private final Department internalDepartment;
    private Long nextPos;

    public DataHandleUtils(String orgId, Map<String, List<WeComUser>> departmentUserMap) {
        this.departmentService = CommonBeanFactory.getBean(DepartmentService.class);
        this.organizationUserService = CommonBeanFactory.getBean(OrganizationUserService.class);
        this.organizationConfigService = CommonBeanFactory.getBean(OrganizationConfigService.class);
        this.orgId = orgId;
        this.departmentUserMap = departmentUserMap;

        assert departmentService != null;
        this.internalDepartment = departmentService.getInternalDepartment(orgId, DepartmentConstants.INTERNAL.name());
        this.nextPos = departmentService.getNextPos(orgId);
    }

    /**
     * 首次同步处理数据
     *
     * @param weComDepartments 微信部门数据
     * @param operatorId       操作人ID
     */
    public void handleAddData(List<WeComDepartment> weComDepartments, String operatorId) {
        this.weComDepartmentTree = WeComDepartment.buildDepartmentTree(internalDepartment.getId(), weComDepartments);
        organizationUserService.deleteUser(orgId, operatorId);
        departmentService.deleteByOrgId(orgId);

        weComDepartmentTree.forEach(wecomDept -> handleTreeAddData(wecomDept, operatorId));

        saveAllEntities();
        organizationConfigService.updateSyncFlag(orgId, DepartmentConstants.WECOM.name());
    }

    private void handleTreeAddData(WeComDepartment weComDepartment, String operatorId) {
        buildData(weComDepartment, operatorId);

        if (CollectionUtils.isNotEmpty(weComDepartment.getChildren())) {
            weComDepartment.getChildren().forEach(department ->
                    handleTreeAddData(department, operatorId)
            );
        }
    }

    /**
     * 构建数据
     *
     * @param weComDepartment 微信部门
     * @param operatorId      操作人ID
     */
    public void buildData(WeComDepartment weComDepartment, String operatorId) {
        List<WeComUser> weComUsers = getWecomUsers(weComDepartment);
        buildDepartment(weComDepartment, operatorId);
        buildUser(weComDepartment, weComUsers, operatorId);
    }

    /**
     * 首次同步构建部门信息
     *
     * @param weComDepartment 微信部门
     * @param operatorId      操作人ID
     */
    public void buildDepartment(WeComDepartment weComDepartment, String operatorId) {
        long currentTime = System.currentTimeMillis();

        if (weComDepartment.getParentId() == 0) {
            Department department = new Department();
            department.setId(internalDepartment.getId());
            department.setName(weComDepartment.getName());
            department.setResourceId(weComDepartment.getId().toString());
            department.setUpdateUser(operatorId);
            department.setUpdateTime(currentTime);
            updateDepartments.add(department);
        } else {
            Department department = new Department();
            department.setId(weComDepartment.getCrmId());
            department.setName(weComDepartment.getName());
            department.setOrganizationId(orgId);
            department.setParentId(weComDepartment.getCrmParentId());
            department.setPos(nextPos);
            department.setResource(DepartmentConstants.WECOM.name());
            department.setResourceId(weComDepartment.getId().toString());
            department.setCreateUser(operatorId);
            department.setUpdateUser(operatorId);
            department.setCreateTime(currentTime);
            department.setUpdateTime(currentTime);
            addDepartments.add(department);
            nextPos += NodeSortUtils.DEFAULT_NODE_INTERVAL_POS;
        }
    }

    /**
     * 首次同步构建用户信息
     *
     * @param weComDepartment 微信部门
     * @param weComUsers      微信用户列表
     * @param operatorId      操作人ID
     */
    public void buildUser(WeComDepartment weComDepartment, List<WeComUser> weComUsers, String operatorId) {
        if (CollectionUtils.isEmpty(weComUsers)) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        weComUsers.forEach(weComUser -> {
            String id = IDGenerator.nextStr();

            // 基本信息
            User user = createUser(id, weComUser, operatorId, currentTime);
            addUsers.add(user);

            // 拓展信息
            UserExtend userExtend = createUserExtend(id, weComUser);
            userExtends.add(userExtend);

            // 组织用户关系
            OrganizationUser orgUser = createOrganizationUser(id, weComDepartment, weComUser, operatorId, currentTime);
            organizationUsers.add(orgUser);

            // 部门负责人信息
            addDepartmentCommanderIfNeeded(id, weComDepartment, weComUser, operatorId, currentTime);
        });
    }

    /**
     * 多次同步更新数据
     *
     * @param weComDepartments 微信部门数据
     * @param operatorId       操作人ID
     */
    public void handleUpdateData(List<WeComDepartment> weComDepartments, String operatorId) {
        // 获取用户列表
        List<OrganizationUser> userList = organizationUserService.getUserByOrgId(orgId);

        // 微信全量用户
        List<WeComUser> wecomUserList = departmentUserMap.values().stream()
                .flatMap(List::stream)
                .toList();

        // 需要禁用的用户（企业微信不存在而系统存在的用户）
        List<OrganizationUser> disableUserList = userList.stream()
                .filter(user -> wecomUserList.stream()
                        .noneMatch(weComUser -> StringUtils.equalsAnyIgnoreCase(weComUser.getUserId(), user.getResourceUserId())))
                .collect(Collectors.toList());

        organizationUserService.disableUsers(disableUserList, operatorId);

        // 当前系统数据
        List<Department> currentDepartmentList = departmentService.getDepartmentByOrgId(orgId);
        List<OrganizationUser> currentUserList = organizationUserService.getUserByOrgId(orgId);
        List<DepartmentCommander> currentCommander = departmentService.getDepartmentCommander(currentUserList);

        if (CollectionUtils.isNotEmpty(currentDepartmentList)) {
            weComDepartmentTree = WeComDepartment.buildDepartmentTreeMultiple(
                    internalDepartment.getId(),
                    currentDepartmentList,
                    weComDepartments
            );
        }

        weComDepartmentTree.forEach(weComDepartment ->
                handleTreeUpdateData(weComDepartment, operatorId, currentDepartmentList, currentUserList, currentCommander)
        );

        saveAllEntities();
    }

    private void handleTreeUpdateData(WeComDepartment weComDepartment, String operatorId,
                                      List<Department> currentDepartmentList,
                                      List<OrganizationUser> currentUserList,
                                      List<DepartmentCommander> currentCommander) {
        buildUpdateData(weComDepartment, operatorId, currentDepartmentList, currentUserList, currentCommander);

        if (CollectionUtils.isNotEmpty(weComDepartment.getChildren())) {
            weComDepartment.getChildren().forEach(department ->
                    handleTreeUpdateData(department, operatorId, currentDepartmentList, currentUserList, currentCommander)
            );
        }
    }

    private void buildUpdateData(WeComDepartment weComDepartment, String operatorId,
                                 List<Department> currentDepartmentList,
                                 List<OrganizationUser> currentUserList,
                                 List<DepartmentCommander> currentCommander) {
        List<WeComUser> weComUsers = getWecomUsers(weComDepartment);

        // 构建更新和新增部门参数
        buildUpdateAndAddDepartment(weComDepartment, operatorId, currentDepartmentList);

        // 构建更新和新增用户参数
        buildUpdateAndAddUser(weComDepartment, weComUsers, operatorId, currentUserList, currentDepartmentList, currentCommander);
    }

    /**
     * 构建更新和新增用户数据
     */
    private void buildUpdateAndAddUser(WeComDepartment weComDepartment, List<WeComUser> weComUsers, String operatorId,
                                       List<OrganizationUser> currentUserList,
                                       List<Department> currentDepartmentList,
                                       List<DepartmentCommander> currentCommander) {
        if (CollectionUtils.isEmpty(weComUsers)) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        weComUsers.forEach(weComUser -> {
            // 查找当前用户是否存在
            OrganizationUser organizationUser = findExistingUser(currentUserList, weComUser);

            // 查找部门
            Department department = findExistingDepartment(currentDepartmentList, weComDepartment);
            String departmentId = (department != null) ? department.getId() : weComDepartment.getCrmId();

            if (organizationUser != null) {
                // 更新已有用户
                updateExistingUser(organizationUser, weComUser, departmentId, operatorId, currentTime);

                // 检查并添加部门负责人
                addCommanderIfNeeded(organizationUser.getUserId(), departmentId, weComUser,
                        weComDepartment, currentCommander, operatorId, currentTime);
            } else {
                // 添加新用户
                addNewUser(weComUser, weComDepartment, departmentId, operatorId, currentTime);
            }
        });
    }

    /**
     * 同步构建更新和新增部门数据
     */
    private void buildUpdateAndAddDepartment(WeComDepartment weComDepartment, String operatorId, List<Department> currentDepartmentList) {
        if (weComDepartment.getId() == 1) {
            // 更新根部门
            Department department = new Department();
            department.setId(internalDepartment.getId());
            department.setName(weComDepartment.getName());
            department.setResourceId(weComDepartment.getId().toString());
            department.setUpdateUser(operatorId);
            department.setUpdateTime(System.currentTimeMillis());
            updateDepartments.add(department);
        } else {
            // 处理子部门
            buildChildrenDepartment(weComDepartment, operatorId, currentDepartmentList);
        }
    }

    /**
     * 构建子部门数据
     */
    private void buildChildrenDepartment(WeComDepartment weComDepartment, String operatorId, List<Department> currentDepartmentList) {
        long currentTime = System.currentTimeMillis();

        if (CollectionUtils.isEmpty(currentDepartmentList)) {
            // 如果当前没有部门数据，则添加新部门
            addNewDepartment(weComDepartment, operatorId, currentTime);
        } else {
            // 查找是否已存在该部门
            Department existingDept = currentDepartmentList.stream()
                    .filter(dept -> StringUtils.equalsAnyIgnoreCase(dept.getResourceId(), weComDepartment.getId().toString()))
                    .findFirst()
                    .orElse(null);

            if (existingDept != null) {
                // 更新已有部门
                updateExistingDepartment(existingDept, weComDepartment, currentDepartmentList, operatorId, currentTime);
            } else {
                // 添加新部门
                addNewDepartment(weComDepartment, operatorId, currentTime);
            }
        }
    }

    // 辅助方法

    private List<WeComUser> getWecomUsers(WeComDepartment weComDepartment) {
        return departmentUserMap.getOrDefault(weComDepartment.getId().toString(), new ArrayList<>());
    }

    private void saveAllEntities() {
        departmentService.save(addDepartments);
        departmentService.update(updateDepartments);
        organizationUserService.save(addUsers, userExtends, organizationUsers, departmentCommanders);
        organizationUserService.update(updateUsers, updateOrganizationUsers);
    }

    private User createUser(String id, WeComUser weComUser, String operatorId, long timestamp) {
        User user = new User();
        user.setId(id);
        user.setName(weComUser.getName());
        user.setPhone(weComUser.getMobile());
        user.setEmail(weComUser.getEmail());
        user.setPassword(CodingUtils.md5(orgId + id));
        user.setGender(weComUser.getGender() != null && weComUser.getGender() != 1);
        user.setLanguage("zh_CN");
        user.setCreateTime(timestamp);
        user.setCreateUser(operatorId);
        user.setUpdateTime(timestamp);
        user.setUpdateUser(operatorId);
        user.setLastOrganizationId(orgId);
        return user;
    }

    private UserExtend createUserExtend(String id, WeComUser weComUser) {
        UserExtend userExtend = new UserExtend();
        userExtend.setId(id);
        userExtend.setAvatar(weComUser.getAvatar());
        return userExtend;
    }

    private OrganizationUser createOrganizationUser(String userId, WeComDepartment weComDepartment,
                                                    WeComUser weComUser, String operatorId, long timestamp) {
        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setId(IDGenerator.nextStr());
        orgUser.setDepartmentId(weComDepartment.getCrmId());
        orgUser.setOrganizationId(orgId);
        orgUser.setUserId(userId);
        orgUser.setResourceUserId(weComUser.getUserId());
        orgUser.setEnable(true);
        orgUser.setPosition(weComUser.getPosition());
        orgUser.setCreateTime(timestamp);
        orgUser.setCreateUser(operatorId);
        orgUser.setUpdateTime(timestamp);
        orgUser.setUpdateUser(operatorId);
        return orgUser;
    }

    private void addDepartmentCommanderIfNeeded(String userId, WeComDepartment weComDepartment,
                                                WeComUser weComUser, String operatorId, long timestamp) {
        int deptIndex = weComUser.getDepartment().indexOf(weComDepartment.getId());

        if (deptIndex >= 0 && weComUser.getIsLeaderInDept().get(deptIndex) == 1) {
            DepartmentCommander commander = new DepartmentCommander();
            commander.setId(IDGenerator.nextStr());
            commander.setUserId(userId);
            commander.setDepartmentId(weComDepartment.getCrmId());
            commander.setCreateTime(timestamp);
            commander.setCreateUser(operatorId);
            commander.setUpdateTime(timestamp);
            commander.setUpdateUser(operatorId);
            departmentCommanders.add(commander);
        }
    }

    private OrganizationUser findExistingUser(List<OrganizationUser> currentUserList, WeComUser weComUser) {
        return Optional.ofNullable(currentUserList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(user -> StringUtils.equalsAnyIgnoreCase(user.getResourceUserId(), weComUser.getUserId()))
                .findFirst()
                .orElse(null);
    }

    private Department findExistingDepartment(List<Department> currentDepartmentList, WeComDepartment weComDepartment) {
        return Optional.ofNullable(currentDepartmentList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(dept -> StringUtils.equalsAnyIgnoreCase(dept.getResourceId(), weComDepartment.getId().toString()))
                .findFirst()
                .orElse(null);
    }

    private void updateExistingUser(OrganizationUser orgUser, WeComUser weComUser,
                                    String departmentId, String operatorId, long timestamp) {
        // 更新用户基本信息
        User updateUser = new User();
        updateUser.setId(orgUser.getUserId());
        updateUser.setName(weComUser.getName());
        updateUser.setUpdateTime(timestamp);
        updateUser.setUpdateUser(operatorId);
        updateUser.setLastOrganizationId(orgId);
        updateUsers.add(updateUser);

        // 更新组织用户关系
        OrganizationUser updateOrgUser = new OrganizationUser();
        updateOrgUser.setId(orgUser.getId());
        updateOrgUser.setDepartmentId(departmentId);
        updateOrgUser.setPosition(weComUser.getPosition());
        updateOrgUser.setUpdateTime(timestamp);
        updateOrgUser.setUpdateUser(operatorId);
        updateOrganizationUsers.add(updateOrgUser);
    }

    private void addCommanderIfNeeded(String userId, String departmentId, WeComUser weComUser,
                                      WeComDepartment weComDepartment, List<DepartmentCommander> currentCommander,
                                      String operatorId, long timestamp) {
        int deptIndex = weComUser.getDepartment().indexOf(weComDepartment.getId());

        if (deptIndex >= 0 && weComUser.getIsLeaderInDept().get(deptIndex) == 1) {
            boolean commanderExists = Optional.ofNullable(currentCommander)
                    .orElse(Collections.emptyList())
                    .stream()
                    .anyMatch(cmd -> StringUtils.equalsAnyIgnoreCase(cmd.getUserId(), userId) &&
                            StringUtils.equalsAnyIgnoreCase(cmd.getDepartmentId(), departmentId));

            if (!commanderExists) {
                DepartmentCommander commander = new DepartmentCommander();
                commander.setId(IDGenerator.nextStr());
                commander.setUserId(userId);
                commander.setDepartmentId(departmentId);
                commander.setCreateTime(timestamp);
                commander.setCreateUser(operatorId);
                commander.setUpdateTime(timestamp);
                commander.setUpdateUser(operatorId);
                departmentCommanders.add(commander);
            }
        }
    }

    private void addNewUser(WeComUser weComUser, WeComDepartment weComDepartment,
                            String departmentId, String operatorId, long timestamp) {
        String id = IDGenerator.nextStr();

        // 创建用户基本信息
        User user = createUser(id, weComUser, operatorId, timestamp);
        addUsers.add(user);

        // 创建用户扩展信息
        UserExtend userExtend = createUserExtend(id, weComUser);
        userExtends.add(userExtend);

        // 创建组织用户关系
        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setId(IDGenerator.nextStr());
        orgUser.setDepartmentId(departmentId);
        orgUser.setOrganizationId(orgId);
        orgUser.setUserId(id);
        orgUser.setResourceUserId(weComUser.getUserId());
        orgUser.setEnable(true);
        orgUser.setPosition(weComUser.getPosition());
        orgUser.setCreateTime(timestamp);
        orgUser.setCreateUser(operatorId);
        orgUser.setUpdateTime(timestamp);
        orgUser.setUpdateUser(operatorId);
        organizationUsers.add(orgUser);

        // 检查并创建部门负责人
        addDepartmentCommanderIfNeeded(id, weComDepartment, weComUser, operatorId, timestamp);
    }

    private void updateExistingDepartment(Department existingDept, WeComDepartment weComDepartment,
                                          List<Department> currentDepartmentList, String operatorId, long timestamp) {
        Department parentDep = currentDepartmentList.stream()
                .filter(dept -> StringUtils.equalsAnyIgnoreCase(dept.getId(), existingDept.getParentId()))
                .findFirst()
                .orElse(null);

        Department updateDept = new Department();
        updateDept.setId(existingDept.getId());
        updateDept.setName(weComDepartment.getName());
        updateDept.setUpdateTime(timestamp);
        updateDept.setUpdateUser(operatorId);
        updateDept.setParentId(parentDep == null ? weComDepartment.getCrmParentId() : parentDep.getId());
        updateDepartments.add(updateDept);
    }

    private void addNewDepartment(WeComDepartment weComDepartment, String operatorId, long timestamp) {
        Department department = new Department();
        department.setId(weComDepartment.getCrmId());
        department.setName(weComDepartment.getName());
        department.setOrganizationId(orgId);
        department.setParentId(weComDepartment.getCrmParentId());
        department.setPos(nextPos);
        department.setResource(DepartmentConstants.WECOM.name());
        department.setResourceId(weComDepartment.getId().toString());
        department.setCreateUser(operatorId);
        department.setUpdateUser(operatorId);
        department.setCreateTime(timestamp);
        department.setUpdateTime(timestamp);
        addDepartments.add(department);
        nextPos += NodeSortUtils.DEFAULT_NODE_INTERVAL_POS;
    }
}