package io.cordys.crm.system.service;

import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleFieldBlob;
import io.cordys.crm.system.dto.field.DateTimeField;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleFieldService {

	@Resource
	private BaseMapper<ModuleField> moduleFieldMapper;
	@Resource
	private DepartmentService departmentService;
	@Resource
	private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;

	/**
	 * 获取不带用户的信息的部门树
	 *
	 * @return List<DeptUserTreeNode>
	 */
	public List<BaseTreeNode> getDeptTree(String orgId) {
		return departmentService.getTree(orgId);
	}

	/**
	 * 修改日期时间类型的字段部分属性
	 */
	public void modifyDateProp() {
		LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ModuleField::getType, FieldType.DATE_TIME.name());
		List<ModuleField> moduleFields = moduleFieldMapper.selectListByLambda(queryWrapper);
		List<String> fieldIds = moduleFields.stream().map(ModuleField::getId).toList();
		if (CollectionUtils.isNotEmpty(fieldIds)) {
			LambdaQueryWrapper<ModuleFieldBlob> blobWrapper = new LambdaQueryWrapper<>();
			blobWrapper.in(ModuleFieldBlob::getId, fieldIds);
			List<ModuleFieldBlob> moduleFieldBlobs = moduleFieldBlobMapper.selectListByLambda(blobWrapper);
			for (ModuleFieldBlob blob : moduleFieldBlobs) {
				DateTimeField dateTimeField = JSON.parseObject(blob.getProp(), DateTimeField.class);
				if (StringUtils.isEmpty(dateTimeField.getDateDefaultType())) {
					dateTimeField.setDateDefaultType("custom");
				}
				blob.setProp(JSON.toJSONString(dateTimeField));
				moduleFieldBlobMapper.updateById(blob);
			}
		}
	}
}
