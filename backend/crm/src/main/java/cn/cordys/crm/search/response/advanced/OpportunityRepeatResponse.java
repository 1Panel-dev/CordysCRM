package cn.cordys.crm.search.response.advanced;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OpportunityRepeatResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "商机名称")
    private String name;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "商机阶段")
    private String stage;

    @Schema(description = "意向产品Ids")
    private List<String> products;

    @Schema(description = "意向产品名称列表")
    private List<String> productNames;

    @Schema(description = "责任人")
    private String owner;

    @Schema(description = "责任人名称")
    private String ownerName;

}
