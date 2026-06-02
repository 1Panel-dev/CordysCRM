package cn.cordys.crm.form.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "custom_form_data")
public class CustomFormData extends BaseModel {

    @Schema(description = "自定义表单ID")
    private String customFormId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "组织id")
    private String organizationId;
}
