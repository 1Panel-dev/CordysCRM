package cn.cordys.crm.customer.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerGetResponse {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "客户名称")
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

    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "协作类型(只读/协作),为空则不是协作人员")
    private String collaborationType;

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;
}
