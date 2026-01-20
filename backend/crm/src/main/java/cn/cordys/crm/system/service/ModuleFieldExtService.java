package cn.cordys.crm.system.service;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.dto.field.DateTimeField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段操作的扩展逻辑
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ModuleFieldExtService {

	@Resource
	private BaseMapper<ModuleForm> formMapper;
	@Resource
	private BaseMapper<ModuleField> fieldMapper;
	@Resource
	private BaseMapper<ModuleFieldBlob> fieldBlobMapper;
	@Resource
	private ModuleFormService moduleFormService;

	public static final String DEFAULT_OPTION_SOURCE = "custom";

	/**
	 * 设置选项字段的默认选项来源
	 */
	public void setDefaultOptionSource() {
		List<ModuleFieldBlob> fieldBlobs = getOptionFieldsBlob();
		fieldBlobs.forEach(fb -> {
			BaseField field = JSON.parseObject(fb.getProp(), BaseField.class);
			if (field instanceof HasOption of) {
				of.setOptionSource(DEFAULT_OPTION_SOURCE);
			}
			fb.setProp(JSON.toJSONString(field));
			fieldBlobMapper.updateById(fb);
		});
	}

	/**
	 * 获取表单附件字段ID集合
	 * @param formKey 表单Key
	 * @param orgId 组织ID
	 * @return 字段ID集合
	 */
	public List<String> getFieldIdsOfForm(String formKey, String orgId) {
		List<BaseField> allFields = moduleFormService.getAllFields(formKey, orgId);
		if (CollectionUtils.isEmpty(allFields)) {
			return List.of();
		}
		return allFields.stream().filter(BaseField::isAttachment)
				.map(BaseField::getId)
				.toList();
	}

	/**
	 * 获取流水号字段规则
	 * @param formKey 表单Key
	 * @param currentOrg 当前组织
	 * @param internalKey 字段Key
	 * @return 字段规则集合
	 */
	public List<String> getSerialFieldRulesByKey(String formKey, String currentOrg, String internalKey) {
		ModuleFieldBlob blob = getFieldBlobByKey(formKey, currentOrg, internalKey);
		if (blob == null) {
			return new ArrayList<>();
		}
		SerialNumberField serialNumberField = JSON.parseObject(blob.getProp(), SerialNumberField.class);
		return serialNumberField.getSerialNumberRules();
	}

	/**
	 * 获取字段选项
	 * @param formKey 表单Key
	 * @param currentOrg 组织ID
	 * @param internalKey 字段内置Key
	 * @return 字段选项
	 */
	public List<OptionProp> getFieldOptions(String formKey, String currentOrg, String internalKey) {
		ModuleFieldBlob blob = getFieldBlobByKey(formKey, currentOrg, internalKey);
		if (blob == null) {
			return new ArrayList<>();
		}
		BaseField field = JSON.parseObject(blob.getProp(), BaseField.class);
		if (field instanceof HasOption of) {
			return of.getOptions();
		}
		return new ArrayList<>();
	}

	/**
	 * 获取日期字段类型
	 * @param formKey 表单Key
	 * @param currentOrg 组织ID
	 * @param internalKey 内部Key
	 * @return 日期字段类型
	 */
	public String getDateFieldType(String formKey, String currentOrg, String internalKey) {
		ModuleFieldBlob blob = getFieldBlobByKey(formKey, currentOrg, internalKey);
		if (blob == null) {
			return "datetime";
		}
		DateTimeField dateField = JSON.parseObject(blob.getProp(), DateTimeField.class);
		return dateField.getDateType();
	}

	/**
	 * 获取选项字段的扩展信息
	 * @return 选项字段列表
	 */
	private List<ModuleFieldBlob> getOptionFieldsBlob() {
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		fieldWrapper.in(ModuleField::getType, List.of(FieldType.SELECT.name(), FieldType.SELECT_MULTIPLE.name(), FieldType.RADIO.name(), FieldType.CHECKBOX.name()));
		List<ModuleField> fields = fieldMapper.selectListByLambda(fieldWrapper);
		List<String> fIds = fields.stream().map(ModuleField::getId).toList();
		return fieldBlobMapper.selectByIds(fIds);
	}

	/**
	 * 根据字段Key获取额外信息
	 * @param formKey 表单Key
	 * @param orgId 组织ID
	 * @param internalKey 字段内置Key
	 * @return 字段大文本
	 */
	private ModuleFieldBlob getFieldBlobByKey(String formKey, String orgId, String internalKey) {
		ModuleForm example = new ModuleForm();
		example.setFormKey(formKey);
		example.setOrganizationId(orgId);
		ModuleForm moduleForm = formMapper.selectOne(example);
		ModuleField fieldExample = new ModuleField();
		fieldExample.setFormId(moduleForm.getId());
		fieldExample.setInternalKey(internalKey);
		ModuleField moduleField = fieldMapper.selectOne(fieldExample);
		if (moduleField == null) {
			return null;
		}
		return fieldBlobMapper.selectByPrimaryKey(moduleField.getId());
	}
}
