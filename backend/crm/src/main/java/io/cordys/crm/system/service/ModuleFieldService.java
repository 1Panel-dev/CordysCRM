package io.cordys.crm.system.service;

import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleFieldService {

	@Resource
	private ExtDepartmentMapper extDepartmentMapper;

	/**
	 * 获取带用户的信息的部门树
	 *
	 * @return List<DeptUserTreeNode>
	 */
	public List<DeptUserTreeNode> getDeptTree(String orgId) {
		List<DeptUserTreeNode> treeNodes = extDepartmentMapper.selectDeptUserTreeNode(orgId);
		return BaseTreeNode.buildTree(treeNodes);
	}
}
