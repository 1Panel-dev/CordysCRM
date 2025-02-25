package io.cordys.crm.system.service;

import io.cordys.common.domain.BaseModel;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleForm;
import io.cordys.crm.system.dto.request.ModuleFieldRequest;
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
	private BaseMapper<ModuleForm> moduleFormMapper;
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
	 * 表单字段是否存在
	 * @param formKey 表单Key
	 * @param fieldId 字段ID
	 * @param orgId 组织ID
	 */
	public void checkFormField(String formKey, String fieldId, String orgId) {
		LambdaQueryWrapper<ModuleForm> formWrapper = new LambdaQueryWrapper<>();
		formWrapper.eq(ModuleForm::getFormKey, formKey)
				.eq(ModuleForm::getOrganizationId, orgId);
		List<ModuleForm> forms = moduleFormMapper.selectListByLambda(formWrapper);
		if (forms.isEmpty()) {
			throw new ArithmeticException(Translator.get("module.field.not_exist"));
		}
		ModuleForm form = forms.getFirst();
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		fieldWrapper.eq(ModuleField::getFormId, form.getId())
				.eq(ModuleField::getId, fieldId);
		List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
		if (fields.isEmpty()) {
			throw new ArithmeticException(Translator.get("module.field.not_exist"));
		}
	}
}
