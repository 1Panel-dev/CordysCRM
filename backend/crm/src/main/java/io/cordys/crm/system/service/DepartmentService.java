package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.NodeSortDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.NodeSortUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.DepartmentCommander;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.dto.log.DepartmentSetCommanderLog;
import io.cordys.crm.system.dto.request.DepartmentAddRequest;
import io.cordys.crm.system.dto.request.DepartmentCommanderRequest;
import io.cordys.crm.system.dto.request.DepartmentRenameRequest;
import io.cordys.crm.system.dto.request.NodeMoveRequest;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


@Service("departmentService")
@Transactional(rollbackFor = Exception.class)
public class DepartmentService extends MoveNodeService {

    @Resource
    private BaseMapper<Department> departmentMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private BaseMapper<DepartmentCommander> departmentCommanderMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;

    /**
     * 获取部门树
     *
     * @return List<BaseTreeNode>
     */
    @Cacheable(value = "dept_tree_cache", key = "#orgId")
    public List<BaseTreeNode> getTree(String orgId) {
        List<BaseTreeNode> departmentList = extDepartmentMapper.selectTreeNode(orgId);
        return BaseTreeNode.buildTree(departmentList);
    }

    /**
     * 添加子部门
     *
     * @param request
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.ADD)
    @CacheEvict(value = "dept_tree_cache", key = "#orgId", beforeInvocation = true)
    public Department addDepartment(DepartmentAddRequest request, String orgId, String userId) {
        //同一层级部门名称唯一
        checkDepartmentName(request.getName(), request.getParentId(), orgId);
        String id = IDGenerator.nextStr();
        Department department = new Department();
        department.setId(id);
        department.setName(request.getName());
        department.setParentId(request.getParentId());
        department.setOrganizationId(orgId);
        department.setPos(getNextPos(orgId));
        department.setCreateTime(System.currentTimeMillis());
        department.setUpdateTime(System.currentTimeMillis());
        department.setCreateUser(userId);
        department.setUpdateUser(userId);
        department.setResource(DepartmentConstants.INTERNAL.name());
        departmentMapper.insert(department);

        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(department)
                .resourceId(id)
                .resourceName(department.getName())
                .build());
        return department;
    }

    private void checkDepartmentName(String name, String parentId, String orgId) {
        if (extDepartmentMapper.countByName(name, parentId, orgId) > 0) {
            throw new GenericException(Translator.get("department_name_exist"));
        }
    }

    public Long getNextPos(String orgId) {
        Long pos = extDepartmentMapper.getNextPosByOrgId(orgId);
        return (pos == null ? 0 : pos) + NodeSortUtils.DEFAULT_NODE_INTERVAL_POS;
    }


    /**
     * 部门重命名
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.UPDATE)
    @CacheEvict(value = "dept_tree_cache", key = "#orgId", beforeInvocation = true)
    public void rename(DepartmentRenameRequest request, String userId, String orgId) {
        Department originalDepartment = checkDepartment(request.getId());
        checkDepartmentName(request.getName(), originalDepartment.getParentId(), originalDepartment.getOrganizationId());

        Department department = BeanUtils.copyBean(new Department(), request);
        department.setUpdateTime(System.currentTimeMillis());
        department.setName(request.getName());
        department.setUpdateUser(userId);
        departmentMapper.updateById(department);

        // 添加日志上下文
        String resourceName = Optional.ofNullable(department.getName()).orElse(originalDepartment.getName());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .originalValue(originalDepartment)
                        .modifiedValue(checkDepartment(request.getId()))
                        .resourceId(request.getId())
                        .resourceName(resourceName)
                        .build()
        );
    }

    private Department checkDepartment(String id) {
        Department department = departmentMapper.selectByPrimaryKey(id);
        if (department == null) {
            throw new GenericException(Translator.get("department.blank"));
        }
        return department;
    }

    /**
     * 设置部门负责人
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.UPDATE)
    public void setCommander(DepartmentCommanderRequest request, String userId) {
        Department department = departmentMapper.selectByPrimaryKey(request.getDepartmentId());
        if (department == null) {
            throw new GenericException(Translator.get("department.blank"));
        }
        // 获取原部门负责人
        String originCommander = setCommanderByDeptId(request.getDepartmentId());

        // 先删除
        deleteCommanderByDeptId(request.getDepartmentId());

        // 再插入
        DepartmentCommander commander = new DepartmentCommander();
        commander.setId(IDGenerator.nextStr());
        commander.setUserId(request.getCommanderId());
        commander.setDepartmentId(request.getDepartmentId());
        commander.setCreateTime(System.currentTimeMillis());
        commander.setUpdateTime(System.currentTimeMillis());
        commander.setCreateUser(userId);
        commander.setUpdateUser(userId);
        departmentCommanderMapper.insert(commander);

        // 设置日志上下文
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(request.getDepartmentId())
                        .resourceName(department.getName())
                        .originalValue(
                                DepartmentSetCommanderLog.builder()
                                        .commander(originCommander)
                                        .build()
                        )
                        .modifiedValue(
                                DepartmentSetCommanderLog.builder()
                                        .commander(request.getCommanderId())
                                        .build()
                        )
                        .build()
        );
    }

    private String setCommanderByDeptId(String departmentId) {
        DepartmentCommander example = new DepartmentCommander();
        example.setDepartmentId(departmentId);
        List<DepartmentCommander> commanders = departmentCommanderMapper.select(example);
        if (CollectionUtils.isNotEmpty(commanders)) {
            return commanders.getFirst().getUserId();
        }
        return null;
    }

    private void deleteCommanderByDeptId(String departmentId) {
        DepartmentCommander example = new DepartmentCommander();
        example.setDepartmentId(departmentId);
        departmentCommanderMapper.delete(example);
    }

    /**
     * 刪除部门
     *
     * @param id
     */
    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.DELETE, resourceId = "{#id}")
    @CacheEvict(value = "dept_tree_cache", key = "#orgId", beforeInvocation = true)
    public void delete(String id, String orgId) {
        Department department = checkDepartment(id);
        if (deleteCheck(id, orgId)) {
            //刪除部門
            departmentMapper.deleteByPrimaryKey(id);
            //todo 部门&责任人关系是否需要删除？ 部门&角色关系是否需要删除？
            // 添加日志上下文
            OperationLogContext.setResourceName(department.getName());
        }
    }


    /**
     * 删除部门前校验
     *
     * @param id
     * @param orgId
     * @return
     */
    public boolean deleteCheck(String id, String orgId) {
        Department department = checkDepartment(id);
        if (StringUtils.equalsAnyIgnoreCase(department.getResource(), DepartmentConstants.INTERNAL.name())
                && StringUtils.equalsAnyIgnoreCase(department.getParentId(), "NONE")) {
            throw new GenericException(Translator.get("department.internal"));
        }
        return extOrganizationUserMapper.countUserByDepartmentId(id, orgId) <= 0;
    }


    /**
     * 保存部门信息
     *
     * @param departments
     */
    public void save(List<Department> departments) {
        departmentMapper.batchInsert(departments);
    }

    /**
     * 删除部门数据
     *
     * @param orgId
     */
    public void deleteByOrgId(String orgId) {
        extDepartmentMapper.deleteByOrgId(orgId);
    }


    /**
     * 创建部门
     *
     * @param departmentPath
     * @param orgId
     * @param departmentTree
     * @param operatorId
     * @param departmentMap
     * @return
     */
    public Map<String, String> createDepartment(List<String> departmentPath, String orgId, List<BaseTreeNode> departmentTree, String operatorId, Map<String, String> departmentMap) {
        departmentPath.forEach(path -> {
            path = "/" + path;
            List<String> depNames = new ArrayList<>(List.of(path.split("/")));
            Iterator<String> itemIterator = depNames.iterator();
            AtomicReference<Boolean> hasNode = new AtomicReference<>(false);
            //当前节点部门名称
            String currentDepName;
            if (depNames.size() <= 1) {
                throw new GenericException(Translator.get("department_create_fail") + ":" + path);
            } else {
                itemIterator.next();
                itemIterator.remove();
                currentDepName = itemIterator.next().trim();
                departmentTree.forEach(department -> {
                    //根节点是否存在
                    if (StringUtils.equalsIgnoreCase(currentDepName, department.getName())) {
                        hasNode.set(true);
                        //根节点存在，检查子节点是否存在
                        createDepByPathIterator(itemIterator, "/" + currentDepName, department, departmentMap, orgId, operatorId);
                    }
                });
            }
            if (!hasNode.get()) {
                //获取顶级部门id
                BaseTreeNode top = extDepartmentMapper.selectDepartment(currentDepName, orgId);
                createDepByPath(itemIterator, currentDepName, top, orgId, StringUtils.EMPTY, departmentMap, operatorId);
            }
        });
        return departmentMap;
    }

    private void createDepByPathIterator(Iterator<String> itemIterator, String currentDepPath, BaseTreeNode departmentTreeNode, Map<String, String> departmentMap, String orgId, String operatorId) {
        List<BaseTreeNode> children = departmentTreeNode.getChildren();
        if (CollectionUtils.isEmpty(children) || !itemIterator.hasNext()) {
            //没有子节点，根据当前部门创建部门节点
            departmentMap.put(currentDepPath, departmentTreeNode.getId());
            if (itemIterator.hasNext()) {
                createDepByPath(itemIterator, itemIterator.next().trim(), departmentTreeNode, orgId, currentDepPath, departmentMap, operatorId);
            }
            return;
        }
        String nodeName = itemIterator.next().trim();
        AtomicReference<Boolean> hasNode = new AtomicReference<>(false);
        children.forEach(child -> {
            if (StringUtils.equalsIgnoreCase(nodeName, child.getName())) {
                hasNode.set(true);
                createDepByPathIterator(itemIterator, currentDepPath + "/" + child.getName(), child, departmentMap, orgId, operatorId);
            }
        });

        //若子节点中不包含该目标节点，则在该节点下创建
        if (!hasNode.get()) {
            createDepByPath(itemIterator, nodeName, departmentTreeNode, orgId, currentDepPath, departmentMap, operatorId);
        }
    }

    private void createDepByPath(Iterator<String> itemIterator, String departmentName, BaseTreeNode parentDep, String orgId, String currentDepPath, Map<String, String> departmentMap, String operatorId) {
        StringBuilder path = new StringBuilder(currentDepPath);
        path.append("/").append(departmentName.trim());

        //模块id
        String pid;
        if (departmentMap.get(path.toString()) != null) {
            //如果创建过，直接获取模块ID
            pid = departmentMap.get(path.toString());
        } else {
            pid = insertNode(departmentName, parentDep.getId(), orgId, operatorId);
            departmentMap.put(path.toString(), pid);
        }

        while (itemIterator.hasNext()) {
            String nextDepName = itemIterator.next().trim();
            path.append("/").append(nextDepName);
            if (departmentMap.get(path.toString()) != null) {
                pid = departmentMap.get(path.toString());
            } else {
                pid = insertNode(nextDepName, pid, orgId, operatorId);
                departmentMap.put(path.toString(), pid);
            }
        }
    }

    private String insertNode(String departmentName, String parentId, String orgId, String operatorId) {
        String id = IDGenerator.nextStr();
        Department department = new Department();
        department.setId(id);
        department.setName(departmentName);
        department.setParentId(parentId);
        department.setOrganizationId(orgId);
        department.setPos(getNextPos(orgId));
        department.setCreateTime(System.currentTimeMillis());
        department.setUpdateTime(System.currentTimeMillis());
        department.setCreateUser(operatorId);
        department.setUpdateUser(operatorId);
        department.setResource(DepartmentConstants.INTERNAL.name());
        departmentMapper.insert(department);
        return id;
    }


    /**
     * 部门树排序
     *
     * @param request
     * @param operatorId
     * @param orgId
     */
    @CacheEvict(value = "dept_tree_cache", key = "#orgId", beforeInvocation = true)
    public void sort(NodeMoveRequest request, String operatorId, String orgId) {
        NodeSortDTO nodeSortDTO = super.getNodeSortDTO(request,
                extDepartmentMapper::selectBaseTreeById,
                extDepartmentMapper::selectTreeByParentIdAndPosOperator);
        Department department = new Department();
        department.setParentId(nodeSortDTO.getParent().getId());
        department.setId(request.getDragNodeId());
        if (departmentMapper.countByExample(department) == 0) {
            Department moveDepartment = departmentMapper.selectByPrimaryKey(request.getDragNodeId());
            moveDepartment.setParentId(nodeSortDTO.getParent().getId());
            checkDepartmentName(moveDepartment.getName(), moveDepartment.getParentId(), orgId);

            moveDepartment.setUpdateUser(operatorId);
            moveDepartment.setUpdateTime(System.currentTimeMillis());
            departmentMapper.update(moveDepartment);
        }
        super.sort(nodeSortDTO);
    }

    @Override
    public void updatePos(String id, long pos) {
        Department department = new Department();
        department.setPos(pos);
        department.setId(id);
        departmentMapper.update(department);
    }

    @Override
    public void refreshPos(String parentId) {
        List<String> childrenIds = extDepartmentMapper.selectChildrenIds(parentId);
        List<Department> departmentList = new ArrayList<>();
        for (int i = 0; i < childrenIds.size(); i++) {
            String nodeId = childrenIds.get(i);
            Department updateDepartment = new Department();
            updateDepartment.setId(nodeId);
            updateDepartment.setPos((i + 1) * LIMIT_POS);
            departmentList.add(updateDepartment);
        }
        extDepartmentMapper.batchUpdate(departmentList);
    }

    /**
     * 获取内部部门 (XPack 中使用)
     *
     * @param orgId
     * @param resource
     * @return
     */
    public Department getInternalDepartment(String orgId, String resource) {
        return extDepartmentMapper.getInternalDepartment(orgId, resource);
    }

    /**
     * 更新部门信息
     *
     * @param departments
     */
    public void update(List<Department> departments) {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtDepartmentMapper mapper = sqlSession.getMapper(ExtDepartmentMapper.class);
        for (Department department : departments) {
            mapper.updateDepartment(department);
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }


    /**
     * 获取全部部门
     *
     * @param orgId
     * @return
     */
    public List<Department> getDepartmentByOrgId(String orgId) {
        return extDepartmentMapper.getDepartmentByOrgId(orgId);
    }


    /**
     * 获取责任人
     *
     * @param userList
     * @return
     */
    public List<DepartmentCommander> getDepartmentCommander(List<OrganizationUser> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        List<String> userIds = userList.stream()
                .map(OrganizationUser::getUserId)
                .toList();
        return extDepartmentMapper.getDepartmentCommander(userIds);
    }

    public void deleteDepartments(List<Department> departmentList) {
        if (CollectionUtils.isNotEmpty(departmentList)) {
            List<String> ids = departmentList.stream()
                    .filter(department -> !StringUtils.equalsAnyIgnoreCase(department.getParentId(), "NONE"))
                    .map(Department::getId).toList();
            if (CollectionUtils.isNotEmpty(ids)) {
                extDepartmentMapper.deleteDepartmentByIds(ids);
            }
        }
    }

    /**
     * 获取部门选项
     * @param ids 部门ID集合
     * @return 部门选项
     */
    public List<Department> getDepartmentOptionsByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        return departmentMapper.selectByIds(ids.toArray(new String[0]));
    }
}