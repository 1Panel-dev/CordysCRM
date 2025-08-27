package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserImportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "部门id")
    private String departmentId;
    @Schema(description = "姓名")
    private String name;
}
