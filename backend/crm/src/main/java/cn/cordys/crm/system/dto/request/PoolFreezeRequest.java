package cn.cordys.crm.system.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PoolFreezeRequest {

    @NotBlank
    @Schema(description = "线索或客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "冻结天数，默认7天，0表示永久冻结，最大1000天")
    private Integer freezeDays = 7;

    @NotBlank(message = "{pool.freeze.reason.not_blank}")
    @Size(max = 300, message = "{pool.freeze.reason.length}")
    @Schema(description = "冻结原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @JsonIgnore
    @AssertTrue(message = "{pool.freeze.days.invalid}")
    public boolean isFreezeDaysValid() {
        return isPermanentFreeze()
                || freezeDays != null && freezeDays >= 1 && freezeDays <= 1000;
    }

    @JsonIgnore
    public boolean isPermanentFreeze() {
        return Integer.valueOf(0).equals(freezeDays);
    }
}
