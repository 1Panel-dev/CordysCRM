package io.cordys.crm.system.mapper;

import io.cordys.common.dto.*;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.DepartmentCommander;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ExtDepartmentMapper {

    List<BaseTreeNode> selectTreeNode(@Param("orgId") String orgId);

    List<DeptUserTreeNode> selectDeptUserTreeNode(@Param("orgId") String orgId);

    Long getNextPosByOrgId(@Param("orgId") String orgId);

    List<String> getUserIdsByDeptIds(@Param("deptIds") List<String> deptIds);

    List<OptionDTO> getIdNameByIds(@Param("ids") List<String> ids);

    void deleteByOrgId(@Param("orgId") String orgId);

    List<Department> selectAllDepartment(@Param("orgId") String orgId);

    BaseTreeNode selectDepartment(@Param("name") String name, @Param("orgId") String orgId);

    int countByName(@Param("name") String name, @Param("parentId") String parentId, @Param("orgId") String orgId);

    BaseTree selectBaseTreeById(@Param("dragNodeId") String dragNodeId);

    BaseTree selectTreeByParentIdAndPosOperator(@Param("nodeSortQueryParam") NodeSortQueryParam nodeSortQueryParam);

    List<String> selectChildrenIds(@Param("parentId") String parentId);

    List<String> selectChildrenByIds(@Param("deptIds") List<String> deptIds);

    void batchUpdate(@Param("departmentList") List<Department> departmentList);

    Department getInternalDepartment(@Param("orgId") String orgId, @Param("resource") String resource);

    void updateDepartment(@Param("department") Department department);

    List<Department> getDepartmentByOrgId(@Param("orgId") String orgId);

    List<DepartmentCommander> getDepartmentCommander(@Param("userIds") List<String> userIds);

    void deleteDepartmentByIds(@Param("ids") List<String> ids);
}
