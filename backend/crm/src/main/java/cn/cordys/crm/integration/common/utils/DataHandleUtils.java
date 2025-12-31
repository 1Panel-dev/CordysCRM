package cn.cordys.crm.integration.common.utils;

import cn.cordys.common.constants.ThirdConfigTypeConstants;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.NodeSortUtils;
import cn.cordys.crm.integration.sync.dto.ThirdDepartment;
import cn.cordys.crm.integration.sync.dto.ThirdUser;
import cn.cordys.crm.system.domain.*;
import cn.cordys.crm.system.dto.request.MessageTaskBatchRequest;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.MessageTaskService;
import cn.cordys.crm.system.service.OrganizationConfigService;
import cn.cordys.crm.system.service.OrganizationUserService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.util.*;
import java.util.stream.Collectors;

public class DataHandleUtils {

    private final String orgId;
    private final DepartmentService departmentService;
    private final OrganizationUserService organizationUserService;
    private final OrganizationConfigService organizationConfigService;
    private final MessageTaskService messageTaskService;
    private final List<Department> addDepartments = new ArrayList<>();
    private final List<Department> updateDepartments = new ArrayList<>();
    private final List<User> addUsers = new ArrayList<>();
    private final List<User> updateUsers = new ArrayList<>();
    private final List<UserExtend> userExtends = new ArrayList<>();
    private final List<OrganizationUser> organizationUsers = new ArrayList<>();
    private final List<OrganizationUser> updateOrganizationUsers = new ArrayList<>();
    private final Map<String, List<ThirdUser>> departmentUserMap;
    private final List<DepartmentCommander> departmentCommanders = new ArrayList<>();
    private final Department internalDepartment;
    private final String type;
    private List<ThirdDepartment> thirdDepartmentTree;
    private Long nextPos;


    public DataHandleUtils(String orgId, Map<String, List<ThirdUser>> departmentUserMap, String type) {
        this.departmentService = CommonBeanFactory.getBean(DepartmentService.class);
        this.organizationUserService = CommonBeanFactory.getBean(OrganizationUserService.class);
        this.organizationConfigService = CommonBeanFactory.getBean(OrganizationConfigService.class);
        this.messageTaskService = CommonBeanFactory.getBean(MessageTaskService.class);
        this.orgId = orgId;
        this.departmentUserMap = departmentUserMap;

        assert departmentService != null;
        this.internalDepartment = departmentService.getInternalDepartment(orgId, ThirdConfigTypeConstants.INTERNAL.name());
        this.nextPos = departmentService.getNextPos(orgId);
        this.type = type;
    }

    /**
     * 首次同步处理数据
     *
     * @param thirdDepartments 微信部门数据
     * @param operatorId       操作人ID
     */
    public void handleAddData(List<ThirdDepartment> thirdDepartments, String operatorId, String orgId, String type) {
        this.thirdDepartmentTree = ThirdDepartment.buildDepartmentTree(internalDepartment.getId(), thirdDepartments);
        organizationUserService.deleteUser(orgId, operatorId);
        departmentService.deleteByOrgId(orgId);

        clearMessageConfig(orgId, operatorId);

        thirdDepartmentTree.forEach(wecomDept -> handleTreeAddData(wecomDept, operatorId));

        saveAllEntities();
        organizationConfigService.updateSyncFlag(orgId, type, true);
    }

    //删除消息配置的数据
    private void clearMessageConfig(String orgId, String operatorId) {
        MessageTaskBatchRequest request = new MessageTaskBatchRequest();
        request.setWeComEnable(false);
        request.setEmailEnable(false);
        request.setLarkEnable(false);
        messageTaskService.batchSaveMessageTask(request, orgId, operatorId);
    }

    private void handleTreeAddData(ThirdDepartment thirdDepartment, String operatorId) {
        buildData(thirdDepartment, operatorId);

        if (CollectionUtils.isNotEmpty(thirdDepartment.getChildren())) {
            thirdDepartment.getChildren().stream()
                    .sorted(Comparator.comparing(ThirdDepartment::getOrder).reversed())
                    .forEach(department ->
                            handleTreeAddData(department, operatorId)
                    );
        }
    }

    /**
     * 构建数据
     *
     * @param thirdDepartment 微信部门
     * @param operatorId      操作人ID
     */
    public void buildData(ThirdDepartment thirdDepartment, String operatorId) {
        List<ThirdUser> thirdUsers = getThirdUsers(thirdDepartment);
        buildDepartment(thirdDepartment, operatorId);
        buildUser(thirdDepartment, thirdUsers, operatorId);
    }

    /**
     * 首次同步构建部门信息
     *
     * @param thirdDepartment 微信部门
     * @param operatorId      操作人ID
     */
    public void buildDepartment(ThirdDepartment thirdDepartment, String operatorId) {
        long currentTime = System.currentTimeMillis();

        if (thirdDepartment.getIsRoot()) {
            Department department = new Department();
            department.setId(internalDepartment.getId());
            department.setName(thirdDepartment.getName());
            department.setResourceId(thirdDepartment.getId());
            department.setUpdateUser(operatorId);
            department.setUpdateTime(currentTime);
            updateDepartments.add(department);
        } else {
            Department department = new Department();
            department.setId(thirdDepartment.getCrmId());
            department.setName(thirdDepartment.getName());
            department.setOrganizationId(orgId);
            department.setParentId(thirdDepartment.getCrmParentId());
            department.setPos(nextPos);
            department.setResource(type);
            department.setResourceId(thirdDepartment.getId());
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
     * @param thirdDepartment 部门
     * @param thirdUsers      用户列表
     * @param operatorId      操作人ID
     */
    public void buildUser(ThirdDepartment thirdDepartment, List<ThirdUser> thirdUsers, String operatorId) {
        if (CollectionUtils.isEmpty(thirdUsers)) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        thirdUsers.forEach(thirdUser -> {
            String id = IDGenerator.nextStr();

            if (CollectionUtils.isNotEmpty(organizationUsers)) {
                OrganizationUser organizationUser = organizationUsers.stream().filter(user -> Strings.CI.equals(user.getResourceUserId(), thirdUser.getUserId()))
                        .findFirst().orElse(null);
                if (organizationUser != null) {
                    return;
                }
            }

            // 基本信息
            User user = createUser(id, thirdUser, operatorId, currentTime);
            addUsers.add(user);

            // 拓展信息
            UserExtend userExtend = createUserExtend(id, thirdUser);
            userExtends.add(userExtend);

            // 组织用户关系
            OrganizationUser orgUser = createOrganizationUser(id, thirdDepartment, thirdUser, operatorId, currentTime);
            organizationUsers.add(orgUser);

            // 部门负责人信息
            addDepartmentCommanderIfNeeded(id, thirdDepartment, thirdUser, operatorId, currentTime);
        });
    }

    /**
     * 多次同步更新数据
     *
     * @param thirdDepartments 微信部门数据
     * @param operatorId       操作人ID
     */
    public void handleUpdateData(List<ThirdDepartment> thirdDepartments, String operatorId) {
        // 获取用户列表
        List<OrganizationUser> userList = organizationUserService.getUserByOrgId(orgId);

        // 微信全量用户
        List<ThirdUser> thirdUserList = departmentUserMap.values().stream()
                .flatMap(List::stream)
                .toList();

        // 需要禁用的用户（企业微信不存在而系统存在的用户）
        List<OrganizationUser> disableUserList = userList.stream()
                .filter(user -> thirdUserList.stream()
                        .noneMatch(thirdUser -> Strings.CI.equalsAny(thirdUser.getUserId(), user.getResourceUserId())))
                .collect(Collectors.toList());

        organizationUserService.disableUsers(disableUserList, operatorId);

        // 当前系统数据
        List<Department> currentDepartmentList = departmentService.getDepartmentByOrgId(orgId);
        List<OrganizationUser> currentUserList = organizationUserService.getUserByOrgId(orgId);
        List<DepartmentCommander> currentCommander = departmentService.getDepartmentCommander(currentUserList);

        if (CollectionUtils.isNotEmpty(currentDepartmentList)) {
            thirdDepartmentTree = ThirdDepartment.buildDepartmentTreeMultiple(
                    internalDepartment.getId(),
                    currentDepartmentList,
                    thirdDepartments
            );
        }

        thirdDepartmentTree.forEach(thirdDepartment ->
                handleTreeUpdateData(thirdDepartment, operatorId, currentDepartmentList, currentUserList, currentCommander)
        );

        saveAllEntities();
    }

    private void handleTreeUpdateData(ThirdDepartment thirdDepartment, String operatorId,
                                      List<Department> currentDepartmentList,
                                      List<OrganizationUser> currentUserList,
                                      List<DepartmentCommander> currentCommander) {
        buildUpdateData(thirdDepartment, operatorId, currentDepartmentList, currentUserList, currentCommander);

        if (CollectionUtils.isNotEmpty(thirdDepartment.getChildren())) {
            thirdDepartment.getChildren().stream()
                    .sorted(Comparator.comparing(ThirdDepartment::getOrder).reversed())
                    .forEach(department ->
                            handleTreeUpdateData(department, operatorId, currentDepartmentList, currentUserList, currentCommander)
                    );
        }
    }

    private void buildUpdateData(ThirdDepartment thirdDepartment, String operatorId,
                                 List<Department> currentDepartmentList,
                                 List<OrganizationUser> currentUserList,
                                 List<DepartmentCommander> currentCommander) {
        List<ThirdUser> thirdUsers = getThirdUsers(thirdDepartment);

        // 构建更新和新增部门参数
        buildUpdateAndAddDepartment(thirdDepartment, operatorId, currentDepartmentList);

        // 构建更新和新增用户参数
        buildUpdateAndAddUser(thirdDepartment, thirdUsers, operatorId, currentUserList, currentDepartmentList, currentCommander);
    }

    /**
     * 构建更新和新增用户数据
     */
    private void buildUpdateAndAddUser(ThirdDepartment thirdDepartment, List<ThirdUser> thirdUsers, String operatorId,
                                       List<OrganizationUser> currentUserList,
                                       List<Department> currentDepartmentList,
                                       List<DepartmentCommander> currentCommander) {
        if (CollectionUtils.isEmpty(thirdUsers)) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        thirdUsers.forEach(thirdUser -> {
            // 查找当前用户是否存在
            OrganizationUser organizationUser = findExistingUser(currentUserList, thirdUser);

            // 查找部门
            Department department = findExistingDepartment(currentDepartmentList, thirdDepartment);
            String departmentId = (department != null) ? department.getId() : thirdDepartment.getCrmId();

            if (organizationUser != null) {
                // 更新已有用户
                updateExistingUser(organizationUser, thirdUser, departmentId, operatorId, currentTime);

                // 检查并添加部门负责人
                addCommanderIfNeeded(organizationUser.getUserId(), departmentId, thirdUser,
                        currentCommander, operatorId, currentTime);
            } else {
                // 添加新用户
                addNewUser(thirdUser, thirdDepartment, departmentId, operatorId, currentTime);
            }
        });
    }

    /**
     * 同步构建更新和新增部门数据
     */
    private void buildUpdateAndAddDepartment(ThirdDepartment thirdDepartment, String operatorId, List<Department> currentDepartmentList) {
        if (thirdDepartment.getIsRoot()) {
            // 更新根部门
            Department department = new Department();
            department.setId(internalDepartment.getId());
            department.setName(thirdDepartment.getName());
            department.setResourceId(thirdDepartment.getId());
            department.setUpdateUser(operatorId);
            department.setUpdateTime(System.currentTimeMillis());
            updateDepartments.add(department);
        } else {
            // 处理子部门
            buildChildrenDepartment(thirdDepartment, operatorId, currentDepartmentList);
        }
    }

    /**
     * 构建子部门数据
     */
    private void buildChildrenDepartment(ThirdDepartment thirdDepartment, String operatorId, List<Department> currentDepartmentList) {
        long currentTime = System.currentTimeMillis();

        if (CollectionUtils.isEmpty(currentDepartmentList)) {
            // 如果当前没有部门数据，则添加新部门
            addNewDepartment(thirdDepartment, operatorId, currentTime);
        } else {
            // 查找是否已存在该部门
            Department existingDept = currentDepartmentList.stream()
                    .filter(dept -> Strings.CI.equalsAny(dept.getResourceId(), thirdDepartment.getId()))
                    .findFirst()
                    .orElse(null);

            if (existingDept != null) {
                // 更新已有部门
                updateExistingDepartment(existingDept, thirdDepartment, currentDepartmentList, operatorId, currentTime);
            } else {
                // 添加新部门
                addNewDepartment(thirdDepartment, operatorId, currentTime);
            }
        }
    }

    // 辅助方法

    private List<ThirdUser> getThirdUsers(ThirdDepartment thirdDepartment) {
        return departmentUserMap.getOrDefault(thirdDepartment.getId(), new ArrayList<>());
    }

    private void saveAllEntities() {
        departmentService.save(addDepartments);
        departmentService.update(updateDepartments);
        organizationUserService.save(addUsers, userExtends, organizationUsers, departmentCommanders);
        organizationUserService.update(updateUsers, updateOrganizationUsers);
    }

    private User createUser(String id, ThirdUser thirdUser, String operatorId, long timestamp) {
        User user = new User();
        user.setId(id);
        user.setName(thirdUser.getName());
        user.setPhone(thirdUser.getMobile());
        user.setEmail(thirdUser.getEmail());
        user.setPassword(CodingUtils.md5(orgId + id));
        user.setGender(thirdUser.getGender() != null && thirdUser.getGender() != 1);
        user.setLanguage("zh_CN");
        user.setCreateTime(timestamp);
        user.setCreateUser(operatorId);
        user.setUpdateTime(timestamp);
        user.setUpdateUser(operatorId);
        user.setLastOrganizationId(orgId);
        return user;
    }

    private UserExtend createUserExtend(String id, ThirdUser thirdUser) {
        UserExtend userExtend = new UserExtend();
        userExtend.setId(id);
        userExtend.setAvatar(thirdUser.getAvatar());
        return userExtend;
    }

    private OrganizationUser createOrganizationUser(String userId, ThirdDepartment thirdDepartment,
                                                    ThirdUser thirdUser, String operatorId, long timestamp) {
        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setId(IDGenerator.nextStr());
        orgUser.setDepartmentId(thirdDepartment.getCrmId());
        orgUser.setOrganizationId(orgId);
        orgUser.setUserId(userId);
        orgUser.setResourceUserId(thirdUser.getUserId());
        orgUser.setEnable(true);
        orgUser.setPosition(thirdUser.getPosition());
        orgUser.setCreateTime(timestamp);
        orgUser.setCreateUser(operatorId);
        orgUser.setUpdateTime(timestamp);
        orgUser.setUpdateUser(operatorId);
        return orgUser;
    }

    private void addDepartmentCommanderIfNeeded(String userId, ThirdDepartment thirdDepartment,
                                                ThirdUser thirdUser, String operatorId, long timestamp) {

        if (thirdUser.getIsLeaderInDept()) {
            DepartmentCommander commander = new DepartmentCommander();
            commander.setId(IDGenerator.nextStr());
            commander.setUserId(userId);
            commander.setDepartmentId(thirdDepartment.getCrmId());
            commander.setCreateTime(timestamp);
            commander.setCreateUser(operatorId);
            commander.setUpdateTime(timestamp);
            commander.setUpdateUser(operatorId);
            departmentCommanders.add(commander);
        }
    }

    private OrganizationUser findExistingUser(List<OrganizationUser> currentUserList, ThirdUser thirdUser) {
        return Optional.ofNullable(currentUserList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(user -> Strings.CI.equalsAny(user.getResourceUserId(), thirdUser.getUserId()))
                .findFirst()
                .orElse(null);
    }

    private Department findExistingDepartment(List<Department> currentDepartmentList, ThirdDepartment thirdDepartment) {
        return Optional.ofNullable(currentDepartmentList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(dept -> Strings.CI.equalsAny(dept.getResourceId(), thirdDepartment.getId()))
                .findFirst()
                .orElse(null);
    }

    private void updateExistingUser(OrganizationUser orgUser, ThirdUser thirdUser,
                                    String departmentId, String operatorId, long timestamp) {
        // 更新用户基本信息
        User updateUser = new User();
        updateUser.setId(orgUser.getUserId());
        updateUser.setName(thirdUser.getName());
        updateUser.setUpdateTime(timestamp);
        updateUser.setUpdateUser(operatorId);
        updateUser.setLastOrganizationId(orgId);
        updateUsers.add(updateUser);

        // 更新组织用户关系
        OrganizationUser updateOrgUser = new OrganizationUser();
        updateOrgUser.setId(orgUser.getId());
        updateOrgUser.setDepartmentId(departmentId);
        updateOrgUser.setPosition(thirdUser.getPosition());
        updateOrgUser.setUpdateTime(timestamp);
        updateOrgUser.setUpdateUser(operatorId);
        updateOrganizationUsers.add(updateOrgUser);
    }

    private void addCommanderIfNeeded(String userId, String departmentId, ThirdUser thirdUser,
                                      List<DepartmentCommander> currentCommander,
                                      String operatorId, long timestamp) {

        if (thirdUser.getIsLeaderInDept()) {
            boolean commanderExists = Optional.ofNullable(currentCommander)
                    .orElse(Collections.emptyList())
                    .stream()
                    .anyMatch(cmd -> Strings.CI.equalsAny(cmd.getUserId(), userId) &&
                            Strings.CI.equalsAny(cmd.getDepartmentId(), departmentId));

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

    private void addNewUser(ThirdUser thirdUser, ThirdDepartment thirdDepartment,
                            String departmentId, String operatorId, long timestamp) {
        String id = IDGenerator.nextStr();

        if (CollectionUtils.isNotEmpty(organizationUsers)) {
            OrganizationUser organizationUser = organizationUsers.stream().filter(user -> Strings.CI.equals(user.getResourceUserId(), thirdUser.getUserId()))
                    .findFirst().orElse(null);
            if (organizationUser != null) {
                return;
            }
        }

        // 创建用户基本信息
        User user = createUser(id, thirdUser, operatorId, timestamp);
        addUsers.add(user);

        // 创建用户扩展信息
        UserExtend userExtend = createUserExtend(id, thirdUser);
        userExtends.add(userExtend);

        // 创建组织用户关系
        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setId(IDGenerator.nextStr());
        orgUser.setDepartmentId(departmentId);
        orgUser.setOrganizationId(orgId);
        orgUser.setUserId(id);
        orgUser.setResourceUserId(thirdUser.getUserId());
        orgUser.setEnable(true);
        orgUser.setPosition(thirdUser.getPosition());
        orgUser.setCreateTime(timestamp);
        orgUser.setCreateUser(operatorId);
        orgUser.setUpdateTime(timestamp);
        orgUser.setUpdateUser(operatorId);
        organizationUsers.add(orgUser);

        // 检查并创建部门负责人
        addDepartmentCommanderIfNeeded(id, thirdDepartment, thirdUser, operatorId, timestamp);
    }

    private void updateExistingDepartment(Department existingDept, ThirdDepartment thirdDepartment,
                                          List<Department> currentDepartmentList, String operatorId, long timestamp) {
        Department parentDep = currentDepartmentList.stream()
                .filter(dept -> Strings.CI.equalsAny(dept.getId(), existingDept.getParentId()))
                .findFirst()
                .orElse(null);

        Department crmParentDep = currentDepartmentList.stream()
                .filter(dept -> Strings.CI.equalsAny(dept.getId(), thirdDepartment.getCrmParentId()))
                .findFirst()
                .orElse(null);

        Department updateDept = new Department();
        updateDept.setId(existingDept.getId());
        updateDept.setName(thirdDepartment.getName());
        updateDept.setUpdateTime(timestamp);
        updateDept.setUpdateUser(operatorId);
        if (crmParentDep != null) {
            updateDept.setParentId(crmParentDep.getId());
        } else {
            updateDept.setParentId(parentDep == null ? thirdDepartment.getCrmParentId() : parentDep.getId());
        }
        updateDepartments.add(updateDept);
    }

    private void addNewDepartment(ThirdDepartment thirdDepartment, String operatorId, long timestamp) {
        Department department = new Department();
        department.setId(thirdDepartment.getCrmId());
        department.setName(thirdDepartment.getName());
        department.setOrganizationId(orgId);
        department.setParentId(thirdDepartment.getCrmParentId());
        department.setPos(nextPos);
        department.setResource(type);
        department.setResourceId(thirdDepartment.getId());
        department.setCreateUser(operatorId);
        department.setUpdateUser(operatorId);
        department.setCreateTime(timestamp);
        department.setUpdateTime(timestamp);
        addDepartments.add(department);
        nextPos += NodeSortUtils.DEFAULT_NODE_INTERVAL_POS;
    }
}