package cn.cordys.crm.search.response.global;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class GlobalClueResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "线索名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "意向产品")
    private List<String> products;

    @Schema(description = "负责人部门名称")
    private String departmentName;

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "自定义字段集合")
    private List<BaseModuleFieldValue> moduleFields;




}
