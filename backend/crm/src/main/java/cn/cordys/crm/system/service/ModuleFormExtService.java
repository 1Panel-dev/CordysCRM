package cn.cordys.crm.system.service;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
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
	 * 设置选项默认来源 (下拉单选)
	 */
	public void setOptionDefaultSource() {
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		fieldWrapper.in(ModuleField::getType, List.of(FieldType.SELECT.name(), FieldType.SELECT_MULTIPLE.name(), FieldType.RADIO.name(), FieldType.CHECKBOX.name()));
		List<ModuleField> fields = fieldMapper.selectListByLambda(fieldWrapper);
		List<String> fIds = fields.stream().map(ModuleField::getId).toList();
		List<ModuleFieldBlob> fieldBlobs = fieldBlobMapper.selectByIds(fIds);
		fieldBlobs.forEach(fb -> {
			BaseField field = JSON.parseObject(fb.getProp(), BaseField.class);
			if (field instanceof HasOption of) {
				of.setOptionSource(DEFAULT_OPTION_SOURCE);
			}
			fb.setProp(JSON.toJSONString(field));
			fieldBlobMapper.updateById(fb);
		});
	}
}
