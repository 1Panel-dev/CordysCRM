package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "sys_department_commander")
public class DepartmentCommander extends BaseModel {

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "部门id")
    private String departmentId;
}
