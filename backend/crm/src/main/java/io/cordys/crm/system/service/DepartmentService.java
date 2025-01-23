package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogExtraDTO;
import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.ServiceUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.DepartmentCommander;
import io.cordys.crm.system.dto.request.DepartmentAddRequest;
import io.cordys.crm.system.dto.request.DepartmentCommanderRequest;
import io.cordys.crm.system.dto.request.DepartmentRenameRequest;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.wecom.dto.WeComDepartment;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service("departmentService")
@Transactional(rollbackFor = Exception.class)
public class DepartmentService {

    @Resource
    private BaseMapper<Department> departmentMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private BaseMapper<DepartmentCommander> departmentCommanderMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;


    /**
     * 获取部门树
     *
     * @return List<BaseTreeNode>
     */
    public List<BaseTreeNode> getTree(String orgId) {
        List<BaseTreeNode> departmentList = extDepartmentMapper.selectTreeNode(orgId);
        List<BaseTreeNode> baseTreeNodes = BaseTreeNode.buildTree(departmentList);
        return baseTreeNodes;
    }

    /**
     * 添加子部门
     *
     * @param request
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM, type = LogType.ADD, operator = "{{#userId}}", success = "添加部门成功", extra = "{{#newDepartment}}")
    public void addDepartment(DepartmentAddRequest request, String orgId, String userId) {
        String id = IDGenerator.nextStr();
        Department department = new Department();
        department.setId(id);
        department.setName(request.getName());
        department.setParentId(request.getParentId());
        department.setOrganizationId(orgId);
        department.setNum(getNextNum(orgId));
        department.setCreateTime(System.currentTimeMillis());
        department.setUpdateTime(System.currentTimeMillis());
        department.setCreateUser(userId);
        department.setUpdateUser(userId);
        department.setResource(DepartmentConstants.INTERNAL.name());
        departmentMapper.insert(department);

        // 添加日志上下文
        OperationLogContext.putVariable("newDepartment", LogExtraDTO.builder()
                .originalValue(null)
                .modifiedValue(department)
                .resourceId(id)
                .build());
    }

    public Long getNextNum(String orgId) {
        Long num = extDepartmentMapper.getNextNumByOrgId(orgId);
        return (num == null ? 0 : num) + ServiceUtils.NUM_STEP;
    }


    /**
     * 部门重命名
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, resourceId = "{{#request.id}}", operator = "{{#userId}}", success = "修改部门名称成功", extra = "{{#updateDepartment}}")
    public void rename(DepartmentRenameRequest request, String userId) {
        Department originalDepartment = checkDepartment(request.getId());

        Department department = BeanUtils.copyBean(new Department(), request);
        department.setUpdateTime(System.currentTimeMillis());
        department.setName(request.getName());
        department.setUpdateUser(userId);
        departmentMapper.updateById(department);

        // 添加日志上下文
        OperationLogContext.putVariable("updateDepartment", LogExtraDTO.builder()
                .originalValue(originalDepartment)
                .modifiedValue(department)
                .build());

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
    @OperationLog(module = LogModule.SYSTEM, type = LogType.ADD, resourceId = "{{#request.departmentId}}", operator = "{{#userId}}", success = "设置部门负责人成功")
    public void setCommander(DepartmentCommanderRequest request, String userId) {
        Department department = departmentMapper.selectByPrimaryKey(request.getDepartmentId());
        if (department == null) {
            throw new GenericException(Translator.get("department.blank"));
        }
        DepartmentCommander commander = new DepartmentCommander();
        commander.setId(IDGenerator.nextStr());
        commander.setUserId(request.getCommanderId());
        commander.setDepartmentId(request.getDepartmentId());
        commander.setCreateTime(System.currentTimeMillis());
        commander.setUpdateTime(System.currentTimeMillis());
        commander.setCreateUser(userId);
        commander.setUpdateUser(userId);
        departmentCommanderMapper.insert(commander);
    }


    /**
     * 刪除部门
     *
     * @param id
     */
    @OperationLog(module = LogModule.SYSTEM, type = LogType.DELETE, resourceId = "{{#id}}", success = "删除部门成功", extra = "{{#updateDepartment}}")
    public void delete(String id, String orgId) {
        Department department = checkDepartment(id);
        if (deleteCheck(id, orgId)) {
            //刪除部門
            departmentMapper.deleteByPrimaryKey(id);
            //todo 部门&责任人关系是否需要删除？ 部门&角色关系是否需要删除？
        }
        // 添加日志上下文
        OperationLogContext.putVariable("updateDepartment", LogExtraDTO.builder()
                .originalValue(department)
                .modifiedValue(null)
                .build());
    }


    /**
     * 删除部门前校验
     *
     * @param id
     * @param orgId
     * @return
     */
    public boolean deleteCheck(String id, String orgId) {
        checkDepartment(id);
        if (extOrganizationUserMapper.countUserByDepartmentId(id, orgId) > 0) {
            return false;
        }
        return true;
    }


    /**
     * 同步构建部门信息
     *
     * @param weComDepartment
     * @param operatorId
     * @param orgId
     * @param departmentList
     */
    public void buildDepartment(WeComDepartment weComDepartment, String operatorId, String orgId, List<Department> departmentList) {
        Department department = new Department();
        department.setId(weComDepartment.getCrmId());
        department.setName(weComDepartment.getName());
        department.setOrganizationId(orgId);
        department.setParentId(weComDepartment.getCrmParentId());
        department.setNum(getNextNum(orgId));
        department.setResource(DepartmentConstants.WECOM.name());
        department.setResourceId(weComDepartment.getId().toString());
        department.setCreateUser(operatorId);
        department.setUpdateUser(operatorId);
        department.setCreateTime(System.currentTimeMillis());
        department.setUpdateTime(System.currentTimeMillis());
        departmentList.add(department);
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
}