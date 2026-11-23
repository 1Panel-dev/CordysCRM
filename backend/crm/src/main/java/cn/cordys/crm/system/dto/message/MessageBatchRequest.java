package cn.cordys.crm.system.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MessageBatchRequest {

    @NotEmpty
    @Schema(description = "ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> ids;

    @Schema(description = "接收方式")
    private String receiveMethod;
}
