package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogExtraDTO;
import io.cordys.common.constants.ApplicationNumScope;
import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.uid.NumGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.dto.request.DepartmentAddRequest;
import io.cordys.crm.system.dto.request.DepartmentRenameRequest;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import io.cordys.mybatis.BaseMapper;
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


    /**
     * 获取部门树
     *
     * @return List<BaseTreeNode>
     */
    public List<BaseTreeNode> getTree() {
        List<BaseTreeNode> departmentList = extDepartmentMapper.selectTreeNode();
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

    private Long getNextNum(String orgId) {
        return NumGenerator.nextNum(orgId, ApplicationNumScope.DEPARTMENT);
    }


    /**
     * 部门重命名
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, resourceId = "{{#request.id}}", operator = "{{#userId}}", success = "修改部门名称成功", extra = "{{#updateDepartment}}")
    public void rename(DepartmentRenameRequest request, String userId) {
        Department originalDepartment = departmentMapper.selectByPrimaryKey(request.getId());
        if (originalDepartment == null) {
            throw new GenericException(Translator.get("department.blank"));
        }

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
}