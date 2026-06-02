package cn.cordys.crm.form.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "custom_form_role")
public class CustomFormRole extends BaseModel {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "自定义表单ID")
    private String customFormId;

    @Schema(description = "内置角色key")
    private String internalKey;
}
