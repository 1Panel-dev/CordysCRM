package cn.cordys.crm.product.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @author song-cc-rock
 */

@Data
public class ProductPriceEditRequest {

	@NotBlank
	@Size(max = 32)
	@Schema(description = "id")
	private String id;

	@NotBlank
	@Size(max = 255)
	@Schema(description = "价格表名称")
	private String name;

	@NotBlank
	@Size(max = 32)
	@Schema(description = "状态")
	private String status;

	@Schema(description = "自定义字段值")
	private List<BaseModuleFieldValue> moduleFields;
}
