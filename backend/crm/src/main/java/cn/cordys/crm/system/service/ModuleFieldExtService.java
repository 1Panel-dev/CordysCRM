package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.DateTimeField;
import cn.cordys.crm.system.dto.field.FormulaField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
	 * 刷新回款计划表单字段位置
	 */
	public void refreshPlanFieldPos() {
		ModuleForm example = new ModuleForm();
		example.setFormKey(FormKey.CONTRACT_PAYMENT_PLAN.getKey());
		example.setOrganizationId(OrganizationContext.DEFAULT_ORGANIZATION_ID);
		ModuleForm moduleForm = formMapper.selectOne(example);
		LambdaQueryWrapper<ModuleField> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		lambdaQueryWrapper.eq(ModuleField::getFormId, moduleForm.getId());
		List<ModuleField> fields = fieldMapper.selectListByLambda(lambdaQueryWrapper);
		fields.forEach(field -> {
			if (Strings.CS.equals(field.getInternalKey(), BusinessModuleField.CONTRACT_PAYMENT_PLAN_NAME.getKey())) {
				field.setPos(1L);
			} else {
				field.setPos(field.getPos() + 1);
			}
			fieldMapper.updateById(field);
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
	public List<String> getSerialFieldRulesByKey(String formKey, String currentOrg, String internalKey, String formulaPrefix) {
		ModuleFieldBlob blob = getFieldBlobByKey(formKey, currentOrg, internalKey);
		if (blob == null) {
			return new ArrayList<>();
		}
		SerialNumberField serialNumberField = JSON.parseObject(blob.getProp(), SerialNumberField.class);
		return serialNumberField.getSerialNumberRules(formulaPrefix);
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

	/**
	 * 修改报价单产品字段的汇总列（兼容旧版本数据）
	 */
	public void modifySubProductSumColumn() {
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		fieldWrapper.like(ModuleField::getType, "SUB_");
		List<ModuleField> fields = fieldMapper.selectListByLambda(fieldWrapper);
		if (CollectionUtils.isEmpty(fields)) {
			return;
		}
		List<String> ids = fields.stream().map(ModuleField::getId).toList();
		List<ModuleFieldBlob> fbs = fieldBlobMapper.selectByIds(ids);
		if (CollectionUtils.isEmpty(fbs)) {
			return;
		}
		for (ModuleFieldBlob fb : fbs) {
			SubField subField = JSON.parseObject(fb.getProp(), SubField.class);
			if (subField == null || CollectionUtils.isEmpty(subField.getSumColumns())) {
				continue;
			}
			List<String> sumColumns = new ArrayList<>();
			subField.getSumColumns().forEach(col -> {
				if (Strings.CS.equals(col, BusinessModuleField.QUOTATION_TOTAL_AMOUNT.getBusinessKey())) {
					sumColumns.add("sumAmount");
				} else if (Strings.CS.contains(col, "_ref_")) {
					sumColumns.add(col.split("ref_")[1]);
				} else {
					sumColumns.add(col);
				}
			});
			subField.setSumColumns(sumColumns);
			fb.setProp(JSON.toJSONString(subField));
			fieldBlobMapper.updateById(fb);
		}
	}

	public void modifyInternalSubSumColumn() {
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		fieldWrapper.in(ModuleField::getInternalKey, List.of("contractProducts", "quotationProducts"));
		List<ModuleField> fields = fieldMapper.selectListByLambda(fieldWrapper);
		List<ModuleFieldBlob> moduleFieldBlobs = fieldBlobMapper.selectByIds(fields.stream().map(ModuleField::getId).toList());
		for (ModuleFieldBlob moduleFieldBlob : moduleFieldBlobs) {
			SubField subField = JSON.parseObject(moduleFieldBlob.getProp(), SubField.class);
			if (subField == null || CollectionUtils.isEmpty(subField.getSumColumns())) {
				return;
			}
			Map<String, String> fieldKeyMap = subField.getSubFields().stream().filter(f -> StringUtils.isNotEmpty(f.getInternalKey()))
					.collect(Collectors.toMap(BaseField::getInternalKey, BaseField::getId));
			List<String> sumColumns = new ArrayList<>();
			subField.getSumColumns().forEach(col -> {
				if (Strings.CS.equals(col, "sumAmount")) {
					if (fieldKeyMap.containsKey("quotationAmount")) {
						sumColumns.add(fieldKeyMap.get("quotationAmount"));
					} else {
						sumColumns.add(fieldKeyMap.get("contractProductSumAmount"));
					}
				} else if (Strings.CS.equals(col, "price")) {
					sumColumns.add(fieldKeyMap.get("contractProductAmount"));
				} else {
					sumColumns.add(col);
				}
			});
			subField.setSumColumns(sumColumns);
			moduleFieldBlob.setProp(JSON.toJSONString(subField));
			fieldBlobMapper.updateById(moduleFieldBlob);
		}
	}

	public void modifyInternalSubCalcFormula() {
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		fieldWrapper.in(ModuleField::getInternalKey, List.of("contractProducts", "quotationProducts"));
		List<ModuleField> fields = fieldMapper.selectListByLambda(fieldWrapper);
		List<ModuleFieldBlob> moduleFieldBlobs = fieldBlobMapper.selectByIds(fields.stream().map(ModuleField::getId).toList());
		String quotationAmountId = null;
		for (ModuleFieldBlob moduleFieldBlob : moduleFieldBlobs) {
			SubField subField = JSON.parseObject(moduleFieldBlob.getProp(), SubField.class);
			if (subField == null) {
				return;
			}
			Map<String, String> fieldKeyMap = subField.getSubFields().stream().filter(f -> StringUtils.isNotEmpty(f.getInternalKey()))
					.collect(Collectors.toMap(BaseField::getInternalKey, BaseField::getId));
			if (fieldKeyMap.containsKey("quotationAmount")) {
				quotationAmountId = fieldKeyMap.get("quotationAmount");
			}
			subField.getSubFields().forEach(f -> {
				if (f instanceof FormulaField formulaField && StringUtils.isNotEmpty(formulaField.getFormula())) {
					if (fieldKeyMap.containsKey("quotationAmount")) {
						formulaField.setFormula(formulaField.getFormula().replace("sumAmount", fieldKeyMap.get("quotationAmount")));
					} else if (fieldKeyMap.containsKey("contractProductAmount")){
						formulaField.setFormula(formulaField.getFormula().replace("price", fieldKeyMap.get("contractProductAmount")));
					} else if (fieldKeyMap.containsKey("contractProductSumAmount")) {
						formulaField.setFormula(formulaField.getFormula().replace("sumAmount", fieldKeyMap.get("contractProductSumAmount")));
					}

				}
			});
			moduleFieldBlob.setProp(JSON.toJSONString(subField));
			fieldBlobMapper.updateById(moduleFieldBlob);
		}

		LambdaQueryWrapper<ModuleField> totalFieldWrapper = new LambdaQueryWrapper<>();
		totalFieldWrapper.eq(ModuleField::getInternalKey, "quotationTotalAmount");
		List<ModuleField> totalFields = fieldMapper.selectListByLambda(totalFieldWrapper);
		ModuleFieldBlob moduleFieldBlob = fieldBlobMapper.selectByPrimaryKey(totalFields.getFirst().getId());
		FormulaField formulaField = JSON.parseObject(moduleFieldBlob.getProp(), FormulaField.class);
		if (formulaField != null && StringUtils.isNotEmpty(formulaField.getFormula())) {
			formulaField.setFormula(formulaField.getFormula().replace("sumAmount", quotationAmountId));
		}
		moduleFieldBlob.setProp(JSON.toJSONString(formulaField));
		fieldBlobMapper.updateById(moduleFieldBlob);
	}

	public void refreshFormulaOldReferencedId() {
		List<ModuleForm> allForms = formMapper.selectAll(null);
		allForms.forEach(form -> {
			Map<String, String> oldToNewMap = new HashMap<>(8);

			// 外层数据源
			LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
			fieldWrapper.eq(ModuleField::getType, FieldType.DATA_SOURCE.name()).eq(ModuleField::getFormId, form.getId());
			List<ModuleField> sourceFields = fieldMapper.selectListByLambda(fieldWrapper);
			List<ModuleFieldBlob> moduleFieldBlobs = fieldBlobMapper.selectByIds(sourceFields.stream().map(ModuleField::getId).toList());
			for (ModuleFieldBlob fieldBlob : moduleFieldBlobs) {
				DatasourceField sourceField = JSON.parseObject(fieldBlob.getProp(), DatasourceField.class);
				if (CollectionUtils.isEmpty(sourceField.getShowFields())) {
					continue;
				}
				sourceField.getShowFields().forEach(sf -> oldToNewMap.put(sf, sourceField.getId() + "_ref_" + sf));
			}

			// 子表格数据源
			LambdaQueryWrapper<ModuleField> subFieldWrapper = new LambdaQueryWrapper<>();
			subFieldWrapper.like(ModuleField::getType, "SUB_").eq(ModuleField::getFormId, form.getId());
			List<ModuleField> subFields = fieldMapper.selectListByLambda(subFieldWrapper);
			List<ModuleFieldBlob> subFieldBlobs = fieldBlobMapper.selectByIds(subFields.stream().map(ModuleField::getId).toList());
			for (ModuleFieldBlob fieldBlob : subFieldBlobs) {
				SubField subField = JSON.parseObject(fieldBlob.getProp(), SubField.class);
				if (CollectionUtils.isEmpty(subField.getSubFields())) {
					continue;
				}
				subField.getSubFields().forEach(sbf -> {
					if (sbf instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields())) {
						sourceField.getShowFields().forEach(sf -> oldToNewMap.put(sf, sourceField.getId() + "_ref_" + sf));
					}
				});
			}

			/*
			 * 替换公式字段中的旧引用ID (外层计算字段 & 子表格计算字段)
			 */
			LambdaQueryWrapper<ModuleField> formulaFieldWrapper = new LambdaQueryWrapper<>();
			formulaFieldWrapper.eq(ModuleField::getType, FieldType.FORMULA.name()).eq(ModuleField::getFormId, form.getId());
			List<ModuleField> formulaFields = fieldMapper.selectListByLambda(formulaFieldWrapper);
			List<ModuleFieldBlob> formulaFieldBlobs = fieldBlobMapper.selectByIds(formulaFields.stream().map(ModuleField::getId).toList());
			for (ModuleFieldBlob fieldBlob : formulaFieldBlobs) {
				FormulaField formulaField = JSON.parseObject(fieldBlob.getProp(), FormulaField.class);
				if (StringUtils.isEmpty(formulaField.getFormula())) {
					continue;
				}
				oldToNewMap.keySet().forEach(oldId -> {
					if (formulaField.getFormula().contains(oldId)) {
						formulaField.setFormula(formulaField.getFormula().replace(oldId, oldToNewMap.get(oldId)));
					}
				});
				fieldBlob.setProp(JSON.toJSONString(formulaField));
				fieldBlobMapper.updateById(fieldBlob);
			}

			for (ModuleFieldBlob fieldBlob : subFieldBlobs) {
				SubField subField = JSON.parseObject(fieldBlob.getProp(), SubField.class);
				if (CollectionUtils.isEmpty(subField.getSubFields())) {
					continue;
				}
				subField.getSubFields().forEach(sbf -> {
					if (sbf instanceof FormulaField formulaField && StringUtils.isNotEmpty(formulaField.getFormula())) {
						oldToNewMap.keySet().forEach(oldId -> {
							if (formulaField.getFormula().contains(oldId)) {
								formulaField.setFormula(formulaField.getFormula().replace(oldId, oldToNewMap.get(oldId)));
							}
						});
					}
				});
				fieldBlob.setProp(JSON.toJSONString(subField));
				fieldBlobMapper.updateById(fieldBlob);
			}
		});
	}
}
