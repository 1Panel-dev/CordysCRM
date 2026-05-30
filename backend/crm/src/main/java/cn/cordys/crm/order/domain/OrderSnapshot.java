package cn.cordys.crm.order.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
@Table(name = "sales_order_snapshot")
public class OrderSnapshot implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;
	
	@Schema(description = "合同id")
	private String orderId;

	@Schema(description = "表单属性快照")
	private String orderProp;

	@Schema(description = "表单值快照")
	private String orderValue;
}
