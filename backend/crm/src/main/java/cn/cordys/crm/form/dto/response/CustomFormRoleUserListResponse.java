package cn.cordys.crm.form.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomFormRoleUserListResponse {

    @Schema(description = "自定义表单角色用户关联ID")
    private String customFormRoleUserId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "创建时间")
    private Long createTime;
}
