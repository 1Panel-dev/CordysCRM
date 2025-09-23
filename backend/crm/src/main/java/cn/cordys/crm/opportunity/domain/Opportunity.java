package cn.cordys.crm.opportunity.domain;

import cn.cordys.common.domain.BaseModel;
import cn.cordys.common.util.BigDecimalNoTrailingZeroSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
@Table(name = "opportunity")
public class Opportunity extends BaseModel {

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "商机名称")
    private String name;

    @Schema(description = "金额")
    @JsonSerialize(using = BigDecimalNoTrailingZeroSerializer.class)
    private BigDecimal amount;

    @Schema(description = "可能性")
    @JsonSerialize(using = BigDecimalNoTrailingZeroSerializer.class)
    private BigDecimal possible;

    @Schema(description = "意向产品id")
    private List<String> products;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "上次修改前的商机阶段")
    private String lastStage;

    @Schema(description = "商机阶段")
    private String stage;

    @Schema(description = "联系人")
    private String contactId;

    @Schema(description = "责任人")
    private String owner;

    @Schema(description = "最新跟进人")
    private String follower;

    @Schema(description = "最新跟进时间")
    private Long followTime;

    @Schema(description = "结束时间")
    private Long expectedEndTime;

    @Schema(description = "实际结束时间")
    private Long actualEndTime;

    @Schema(description = "失败原因")
    private String failureReason;

}
