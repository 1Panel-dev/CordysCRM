package cn.cordys.crm.clue.dto.request;

import cn.cordys.crm.clue.dto.CluePoolPickRuleDTO;
import cn.cordys.crm.clue.dto.CluePoolRecycleRuleDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CluePoolAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "线索池名称")
    private String name;

    @NotNull
    @Schema(description = "范围ID集合")
    private List<String> scopeIds;

    @NotNull
    @Schema(description = "管理员ID集合")
    private List<String> ownerIds;

    @NotNull
    @Schema(description = "启用/禁用")
    private Boolean enable;

    @NotNull
    @Schema(description = "自动回收")
    private Boolean auto;

    @Schema(description = "领取规则")
    private CluePoolPickRuleDTO pickRule;

    @Schema(description = "回收规则")
    private CluePoolRecycleRuleDTO recycleRule;

    @Schema(description = "隐藏字段ID集合")
    private Set<@NotBlank String> hiddenFieldIds;
}
