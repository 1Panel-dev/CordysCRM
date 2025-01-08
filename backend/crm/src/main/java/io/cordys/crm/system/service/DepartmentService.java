package io.cordys.crm.system.service;

import io.cordys.common.dto.BaseTreeNode;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service("departmentService")
@Transactional(rollbackFor = Exception.class)
public class DepartmentService {

    @Resource
    private ExtDepartmentMapper extDepartmentMapper;


    public List<BaseTreeNode> getTree() {
        List<BaseTreeNode> departmentList = extDepartmentMapper.selectTreeNodeByOrgId();
        List<BaseTreeNode> baseTreeNodes = BaseTreeNode.buildTree(departmentList);
        return baseTreeNodes;
    }
}