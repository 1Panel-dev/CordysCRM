package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "sys_user_view_condition")
public class UserViewCondition extends BaseModel {

    @Schema(description = "视图id")
    private String sysUserViewId;

    @Schema(description = "参数名称")
    private String name;

    @Schema(description = "参数值")
    private String value;

    @Schema(description = "参数值类型")
    private String valueType;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "是否是多选值")
    private Boolean multipleValue;

    @Schema(description = "操作符")
    private String operator;
}
