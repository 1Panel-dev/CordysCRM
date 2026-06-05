package cn.cordys.crm.form.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "custom_form")
public class CustomForm {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "组织id")
    private String organizationId;
}
