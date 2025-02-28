package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "MEMBER")
@EqualsAndHashCode(callSuper = true)
public class MemberField extends BaseField {

    @Schema(description = "是否是多选")
    private Boolean multiple;

    @Schema(description = "是否当前用户")
    private Boolean hasCurrentUser;
}
