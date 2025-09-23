package cn.cordys.crm.customer.dto;

import cn.cordys.crm.system.dto.RuleConditionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPoolRuleDTO {

    @Schema(description = "是否限制领取数量")
    private Boolean limitOnNumber;
    @Schema(description = "领取数量")
    private Integer pickNumber;
    @Schema(description = "是否限制前归属人领取")
    private Boolean limitPreOwner;
    @Schema(description = "领取间隔天数")
    private Integer pickIntervalDays;
    @Schema(description = "到期提醒")
    private Boolean expireNotice;
    @Schema(description = "提前提醒天数")
    private Integer noticeDays;
    @Schema(description = "操作符")
    private String operator;
    @Schema(description = "规则条件集合")
    private List<RuleConditionDTO> conditions;
}
