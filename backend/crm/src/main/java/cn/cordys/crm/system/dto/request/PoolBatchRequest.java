package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PoolBatchRequest {

    @NotEmpty
    @Schema(description = "批量ID集合")
    private List<String> batchIds;
}
