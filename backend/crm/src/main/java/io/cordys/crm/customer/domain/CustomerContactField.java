package io.cordys.crm.customer.domain;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 客户联系人自定义属性
 * 
 * @author jianxing
 * @date 2025-02-24 16:23:32
 */
@Data
@Table(name = "customer_contact_field")
public class CustomerContactField extends BaseModuleFieldValue {
	@Schema(description = "ID")
	private String id;

	@Schema(description = "客户id")
	private String customerContactId;
}
