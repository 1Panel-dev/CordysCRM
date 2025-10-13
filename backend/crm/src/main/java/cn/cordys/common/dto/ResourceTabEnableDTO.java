package cn.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户、商机、线索是否显示所有数据和部门数据 tab
 *
 * @Author: jianxing
 * @CreateTime: 2025-05-15  14:54
 */
@Data
public class ResourceTabEnableDTO {
    @Schema(description = "是否显示所有数据tab")
    private Boolean all = false;
    @Schema(description = "是否显示部门数据tab")
    private Boolean dept = false;

    /**
     * 合并权限
     * @param other 其余数据权限配置
     * @return 合并后的数据权限配置
     */
    public ResourceTabEnableDTO or(ResourceTabEnableDTO other) {
        if (other != null) {
            all  |= other.all;
            dept |= other.dept;
        }
        return this;
    }
}
