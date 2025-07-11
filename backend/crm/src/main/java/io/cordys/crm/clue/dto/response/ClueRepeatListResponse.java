package io.cordys.crm.clue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ClueRepeatListResponse {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "联系人电话")
    private String phone;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "意向产品id")
    private List<String> products;

    @Schema(description = "意向产品名称")
    private List<String> productNameList;

}
