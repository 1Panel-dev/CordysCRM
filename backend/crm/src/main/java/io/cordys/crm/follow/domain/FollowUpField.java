package io.cordys.crm.follow.domain;

import io.cordys.common.domain.BaseModuleFieldValue;
import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;



@Data
@Table(name = "follow_up_field")
public class FollowUpField extends BaseModuleFieldValue {

	@Schema(description = "ID")
	private String id;

	@Schema(description = "跟进记录id")
	private String followUpId;
}
