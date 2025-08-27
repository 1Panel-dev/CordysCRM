package cn.cordys.crm.customer.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 客户历史责任人
 *
 * @author jianxing
 * @date 2025-03-12 10:20:41
 */
@Data
@Table(name = "customer_owner")
public class CustomerOwner {

    @Schema(description = "id")
    private String id;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "责任人")
    private String owner;

    @Schema(description = "领取时间")
    private Long collectionTime;

    @Schema(description = "结束时间")
    private Long endTime;

    @Schema(description = "操作人")
    private String operator;
}
