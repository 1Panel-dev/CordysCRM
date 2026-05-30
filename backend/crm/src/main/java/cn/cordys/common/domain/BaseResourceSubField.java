package cn.cordys.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author song-cc-rock
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResourceSubField extends BaseResourceField {

	@Schema(description = "关联子表格ID")
	private String refSubId;

	@Schema(description = "行ID")
	private String rowId;

	@Schema(description = "行唯一标识")
	private String bizId;
}
