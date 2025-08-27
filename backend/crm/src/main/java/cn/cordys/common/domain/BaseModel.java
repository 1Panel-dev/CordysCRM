package cn.cordys.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class BaseModel implements Serializable {
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Serial
    private static final long serialVersionUID = 1L;

}
