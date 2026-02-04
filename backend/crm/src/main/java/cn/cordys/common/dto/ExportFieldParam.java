package cn.cordys.common.dto;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Data
@Builder
public class ExportFieldParam {

	/**
	 * 子表格ID集合
	 */
	private List<String> subIds;

	/**
	 * 字段配置
	 */
	private Map<String, BaseField> fieldConfigMap;

	/**
	 * 表单配置
	 */
	private ModuleFormConfigDTO formConfig;
}
