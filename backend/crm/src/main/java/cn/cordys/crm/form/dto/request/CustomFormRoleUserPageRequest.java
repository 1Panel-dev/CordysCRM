package cn.cordys.crm.form.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomFormRoleUserPageRequest extends BasePageRequest {

    @NotBlank
    @Schema(description = "自定义表单角色ID")
    private String customFormRoleId;
}
