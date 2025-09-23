package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.domain.Department;
import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonTypeName(value = "DEPARTMENT_MULTIPLE")
@EqualsAndHashCode(callSuper = true)
public class DepartmentMultipleField extends BaseField {
    @Schema(description = "默认值")
    private List<String> defaultValue = new ArrayList<>();

    @Schema(description = "是否当前部门")
    private Boolean hasCurrentUserDept;

    @Schema(description = "默认值初始化选项")
    private List<Department> initialOptions;
}
