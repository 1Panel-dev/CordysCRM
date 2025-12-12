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
	 * 子表格字段ID (目前唯一, 后续扩展)
	 */
	private String subId;

	/**
	 * 字段配置
	 */
	private Map<String, BaseField> fieldConfigMap;

	/**
	 * 表单配置
	 */
	private ModuleFormConfigDTO formConfig;

	/**
	 * 需要合并的列索引
	 */
	private List<Integer> mergeColumns;
}
