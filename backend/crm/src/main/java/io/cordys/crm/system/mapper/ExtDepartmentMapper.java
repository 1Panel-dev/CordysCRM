package io.cordys.crm.system.mapper;

import io.cordys.common.dto.BaseTreeNode;

import java.util.List;


public interface ExtDepartmentMapper {

    List<BaseTreeNode> selectTreeNode();
}
