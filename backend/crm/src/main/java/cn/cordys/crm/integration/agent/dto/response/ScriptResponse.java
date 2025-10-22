package cn.cordys.crm.integration.agent.dto.response;


import cn.cordys.crm.integration.agent.dto.ParameterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ScriptResponse {

    private List<ParameterDTO> parameters;

    @Schema(description = "地址值")
    private String src;

}
