package io.cordys.crm.customer.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 * 客户
 * 
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
@Table(name = "customer")
public class Customer extends BaseModel {

	@Schema(description = "标签")
	private List<String> tags;

	@Schema(description = "是否在公海池")
	private Boolean inSharedPool;

	@Schema(description = "组织id")
	private String organizationId;
}
