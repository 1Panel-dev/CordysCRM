package cn.cordys.crm.system.dto;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据源引用详情信息
 * @author song-cc-rock
 */
@Data
public class DatasourceRefDTO extends DatasourceRefBusinessDetail {

	@Schema(description = "数据源ID")
	private String id;

	@Schema(description = "自定义字段集合")
	private List<BaseModuleFieldValue> moduleFields;

	@Schema(description = "产品子列表")
	private List<Map<String, Object>> products;

	@Schema(description = "选项集合")
	private Map<String, List<OptionDTO>> optionMap;
}
