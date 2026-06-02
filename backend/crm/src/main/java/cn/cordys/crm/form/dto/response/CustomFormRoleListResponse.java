package cn.cordys.crm.form.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomFormRoleListResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "自定义表单ID")
    private String customFormId;

    @Schema(description = "内置角色key")
    private String internalKey;
}
