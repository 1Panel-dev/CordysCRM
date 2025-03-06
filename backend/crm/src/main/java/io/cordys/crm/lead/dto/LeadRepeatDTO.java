package io.cordys.crm.lead.dto;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class LeadRepeatDTO {

    @Schema(description = "线索id")
    private String id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;



}
