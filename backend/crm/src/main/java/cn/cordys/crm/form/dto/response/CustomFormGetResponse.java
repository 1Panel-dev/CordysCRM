package cn.cordys.crm.form.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.form.domain.CustomForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CustomFormGetResponse extends CustomForm {

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "当前用户是否是管理员")
    private Boolean isAdmin;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}
