package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SyncUserRequest {

    @Schema(description = "资源类型：当前第三方平台类型")
    @NotBlank
    private String type;

    @Schema(description = "同步范围ids")
    private List<OptionDTO> syncScope = new ArrayList<>();
}
