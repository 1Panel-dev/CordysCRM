package io.cordys.crm.follow.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;



@Data
@Table(name = "follow_up_field_blob")
public class FollowUpFieldBlob extends BaseModel {

	@Schema(description = "跟进记录id")
	private String followUpId;

	@Schema(description = "自定义属性id")
	private String fieldId;

	@Schema(description = "自定义属性值")
	private String fieldValue;
}
