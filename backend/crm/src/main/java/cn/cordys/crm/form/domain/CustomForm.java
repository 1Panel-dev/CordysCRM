package cn.cordys.crm.form.domain;


import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "custom_form")
public class CustomForm extends BaseModel {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "组织id")
    private String organizationId;
}
