package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class RoleUserOptionResponse {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户名称")
    private String name;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "是否启用")
    private Boolean enabled = true;
}
