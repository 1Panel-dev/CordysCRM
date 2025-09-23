package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PoolBatchRequest {

    @NotNull
    @Schema(description = "批量ID集合")
    private List<String> batchIds;
}
