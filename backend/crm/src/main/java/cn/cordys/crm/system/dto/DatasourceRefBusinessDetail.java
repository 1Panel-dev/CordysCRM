package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据源引用业务详情字段
 * @author song-cc-rock
 */
@Data
public class DatasourceRefBusinessDetail {

	@Schema(description = "名称")
	private String name;

	@Schema(description = "价格")
	private BigDecimal price;

	@Schema(description = "状态")
	private String status;
}
