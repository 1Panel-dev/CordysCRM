package cn.cordys.crm.form.dto.response;

import cn.cordys.crm.form.domain.CustomForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomFormListResponse extends CustomForm {
    @Schema(description = "当前用户是否是管理员")
    private Boolean isAdmin;
    @Schema(description = "当前用户是否有权限创建表单数据")
    private Boolean hasCreateDataPermission;
}
