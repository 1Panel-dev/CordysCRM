package cn.cordys.crm.system.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.condition.FilterCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserViewResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "视图名称")
    private String name;

    @Schema(description = "资源类型(客户/线索/商机)")
    private String resourceType;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "排序")
    private Long pos;

    @Schema(description = "匹配模式(AND/OR)")
    private String searchMode;

    @Schema(description = "筛选条件")
    private List<FilterCondition> conditions;

    @Schema(description = "用于回显的optionMap")
    private Map<String, List<OptionDTO>> optionMap;
}
