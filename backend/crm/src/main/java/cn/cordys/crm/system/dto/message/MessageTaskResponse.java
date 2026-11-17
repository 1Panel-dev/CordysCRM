package cn.cordys.crm.system.dto.message;

import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.message.TemplateEnum;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class MessageTaskResponse {

    @Schema(description = "id")
    private String id;

    @Schema(description = "场景名称")
    private String name;

    @Schema(description = "所属分类")
    private String taskType;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "接收人")
    private String scopeId;

    @Schema(description = "接收人集合")
    private List<ScopeNameDTO> members;

    @Schema(description = "接收方式")
    private String receiveMethod;

    @Schema(description = "触发类型配置json对象")
    private String value;

    @Schema(description = "触发类型配置对象")
    private TriggerTypeConfigDTO config;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "修改人名称")
    private String updateUserName;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "模板")
    private String template;

    @Schema(description = "模板值")
    private List<String> templateValue;

    /**
     * 将 template 中存储的模板枚举名(如 ["BUSINESS_TRANSFER_OLD_OWNER_TEXT",...])转换为对应的 i18n 文案
     */
    public void setTemplateValue() {
        if (StringUtils.isBlank(this.template)) {
            this.templateValue = new ArrayList<>();
            return;
        }
        this.templateValue = JSON.parseArray(this.template, String.class).stream()
                .map(name -> {
                    TemplateEnum templateEnum = TemplateEnum.get(name);
                    return templateEnum == null ? name : Translator.get(templateEnum.getDescription());
                })
                .toList();
    }
}
