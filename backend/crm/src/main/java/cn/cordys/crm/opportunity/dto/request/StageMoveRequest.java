package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class StageMoveRequest {

    @Schema(description = "ids")
    private List<String> ids;
}
