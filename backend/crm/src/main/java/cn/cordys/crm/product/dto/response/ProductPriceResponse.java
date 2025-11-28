package cn.cordys.crm.product.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author song-cc-rock
 */
@Data
public class ProductPriceResponse {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "价格表名称")
    private String name;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "自定义字段集合")
    private List<BaseModuleFieldValue> moduleFields;

	@Schema(description = "产品子列表")
	private List<Map<String, Object>> products = new ArrayList<>();
}
