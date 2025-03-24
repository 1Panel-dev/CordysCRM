package io.cordys.crm.system.service;

import com.alibaba.excel.util.StringUtils;
import io.cordys.common.domain.BaseModel;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.dto.request.ModuleSourceDataRequest;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleFieldService {

	@Resource
	private BaseMapper<ModuleField> moduleFieldMapper;
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

	public List<? extends BaseModel> getSourceData(ModuleSourceDataRequest request, String orgId) {
		// TODO: 返回不同数据源数据
		return null;
	}

	/**
	 * 表单字段类型是否匹配
	 * @param fieldId 字段ID
	 */
	public void isMatchType(String fieldId, String type) {
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		fieldWrapper.eq(ModuleField::getId, fieldId);
		List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
		if (fields.isEmpty()) {
			throw new GenericException(Translator.get("module.field.not_exist"));
		}
		ModuleField field = fields.getFirst();
		if (!StringUtils.equals(type, field.getType())) {
			throw new GenericException(Translator.get("module.field.not_match_type"));
		}
	}
}
