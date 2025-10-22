package cn.cordys.crm.integration.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParameterDTO {

    @Schema(description = "参数")
    private String parameter;

    @Schema(description = "参数值")
    private String value;
}
