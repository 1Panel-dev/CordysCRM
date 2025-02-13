package io.cordys.crm.system.mapper;

import io.cordys.common.dto.*;
import io.cordys.crm.system.domain.Department;
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

    void batchUpdate(@Param("departmentList") List<Department> departmentList);
}
