package cn.cordys.crm.system.dto;

import cn.cordys.crm.contract.domain.BusinessTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据源引用业务详情字段
 * @author song-cc-rock
 */
@Data
public class DatasourceRefBusinessDetail extends BusinessTitle {

	@Schema(description = "价格")
	private BigDecimal price;

	@Schema(description = "状态")
	private String status;
}
