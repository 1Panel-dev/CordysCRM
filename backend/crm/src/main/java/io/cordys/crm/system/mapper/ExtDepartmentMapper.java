package io.cordys.crm.system.mapper;

import io.cordys.common.dto.BaseTreeNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ExtDepartmentMapper {

    List<BaseTreeNode> selectTreeNode(@Param("orgId") String orgId);

    Long getNextNumByOrgId(@Param("orgId") String orgId);
}
