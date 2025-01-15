package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class UserBatchRequest {

    @Schema(description = "ID集合")
    private List<String> ids;

}
