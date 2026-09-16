package cn.cordys.crm.system.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PoolFreezeRequest {

    @NotBlank
    @Schema(description = "线索或客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @NotNull
    @Schema(description = "是否永久冻结", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean permanent = false;

    @Schema(description = "冻结天数，非永久冻结时必填，范围1-1000天")
    private Integer freezeDays = 7;

    @NotBlank(message = "{pool.freeze.reason.not_blank}")
    @Size(max = 300, message = "{pool.freeze.reason.length}")
    @Schema(description = "冻结原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @JsonIgnore
    @AssertTrue(message = "{pool.freeze.days.invalid}")
    public boolean isFreezeDaysValid() {
        return Boolean.TRUE.equals(permanent)
                || freezeDays != null && freezeDays >= 1 && freezeDays <= 1000;
    }
}
