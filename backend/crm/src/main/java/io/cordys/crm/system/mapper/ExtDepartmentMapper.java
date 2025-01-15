package io.cordys.crm.system.mapper;

import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptUserTreeNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ExtDepartmentMapper {

    List<BaseTreeNode> selectTreeNode(@Param("orgId") String orgId);

    List<DeptUserTreeNode> selectDeptUserTreeNode(@Param("orgId") String orgId);

    Long getNextNumByOrgId(@Param("orgId") String orgId);
}
