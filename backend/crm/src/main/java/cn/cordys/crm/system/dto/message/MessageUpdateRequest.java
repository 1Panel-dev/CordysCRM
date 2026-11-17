package cn.cordys.crm.system.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MessageUpdateRequest extends MessageAddRequest{

    @Schema(description = "id")
    private String id;
}
