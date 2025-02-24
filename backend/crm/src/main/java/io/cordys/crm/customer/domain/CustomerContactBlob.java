package io.cordys.crm.customer.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 客户联系人自定义属性大文本
 * 
 * @author jianxing
 * @date 2025-02-24 16:23:32
 */
@Data
@Table(name = "customer_contact_blob")
public class CustomerContactBlob extends BaseModel {

	@Schema(description = "客户id")
	private String customerId;

	@Schema(description = "自定义属性id")
	private String fieldId;

	@Schema(description = "自定义属性值")
	private String fieldValue;
}
