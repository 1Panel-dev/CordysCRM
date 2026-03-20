package cn.cordys.common.dto;

import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.crm.system.dto.field.base.BaseField;
import lombok.Data;

/**
 * 导出字段元数据 (预处理)
 * @author song-cc-rock
 */
@Data
public class FieldExportMeta {

	private String head;

	private BaseField field;

	private AbstractModuleFieldResolver<?> resolver;

	private boolean noResource;

	private String fieldId;

	private String businessKey;

	private String prefixId;
}
