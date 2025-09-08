package cn.cordys.crm.home.dto.request;

import cn.cordys.common.constants.BusinessSearchType;
import cn.cordys.common.constants.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class HomeStatisticBaseSearchRequest {

    @EnumValue(enumClass = BusinessSearchType.class)
    @Schema(description = "搜索类型(ALL/SELF/DEPARTMENT)")
    private String searchType;

    @Schema(description = "部门ID集合")
    private Set<String> deptIds;
}
