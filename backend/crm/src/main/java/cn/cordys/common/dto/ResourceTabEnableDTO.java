package cn.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户、商机、线索是否显示所有数据和部门数据 tab
 * @Author: jianxing
 * @CreateTime: 2025-05-15  14:54
 */
@Data
public class ResourceTabEnableDTO {
    @Schema(description = "是否显示所有数据tab")
    private Boolean all = false;
    @Schema(description = "是否显示部门数据tab")
    private Boolean dept = false;
}
