package cn.cordys.crm.customer.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 客户协作人
 * 
 * @author jianxing
 * @date 2025-03-04 11:06:12
 */
@Data
@Table(name = "customer_collaboration")
public class CustomerCollaboration extends BaseModel {

	@Schema(description = "协作人id")
	private String userId;

	@Schema(description = "客户id")
	private String customerId;

	@Schema(description = "协作类型(只读/协作)")
	private String collaborationType;
}
