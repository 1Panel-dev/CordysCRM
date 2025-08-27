package cn.cordys.crm.system.dto.response.product;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ProductGetResponse {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "产品名称")
    private String name;

    @Schema(description = "价格")
    private BigDecimal price;

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

    @Schema(description = "自定义字段")
    private List<? extends BaseModuleFieldValue> moduleFields;

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;
}
