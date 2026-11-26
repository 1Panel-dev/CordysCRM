package cn.cordys.crm.system.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class MessageAddRequest {

    @Schema(description = "场景名称")
    private String name;

    @Schema(description = "类型")
    private String taskType;

    @Schema(description = "表单类型(所属分类)")
    private String formType;

    @Schema(description = "接收人")
    private List<String> scopeId;

    @Schema(description = "接收方式")
    private String receiveMethod;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "触发类型")
    private TriggerTypeConfigDTO config;

    @Schema(description = "模板")
    private String template;

    @Schema(description = "类型(系统表单 SYSTEM/ 自定义表单 CUSTOM)")
    private String type;

}
