package cn.cordys.crm.form.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CustomFormDataGetResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "自定义表单ID")
    private String customFormId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;

    @Schema(description = "附件集合")
    private Map<String, List<Attachment>> attachmentMap;

    @Schema(description = "当前用户是否是管理员")
    private Boolean isAdmin;
}
