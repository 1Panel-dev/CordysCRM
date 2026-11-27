package cn.cordys.crm.system.dto.message;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageRequest extends BasePageRequest {

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "所属类型")
    private String formType;
}
