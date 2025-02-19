package io.cordys.crm.system.dto.response;

import io.cordys.common.request.ModuleFieldValueDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 * @author guoyuqi
 */
@Data
public class ProductListResponse {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "产品名称")
    private String name;

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
    private List<ModuleFieldValueDTO> moduleFields;
}
