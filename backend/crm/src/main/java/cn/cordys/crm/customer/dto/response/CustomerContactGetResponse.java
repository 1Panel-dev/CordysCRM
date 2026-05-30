package cn.cordys.crm.customer.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Data
public class CustomerContactGetResponse extends CustomerContact {
    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;

    /**
     * 附件集合
     */
    @Schema(description = "附件集合")
    private Map<String, List<Attachment>> attachmentMap;
}
