package cn.cordys.crm.system.service;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ModuleFormExtService {

	@Resource
	private BaseMapper<ModuleField> fieldMapper;
	@Resource
	private BaseMapper<ModuleFieldBlob> fieldBlobMapper;

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
}
