package io.cordys.crm.follow.domain;

import io.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Table(name = "follow_up_plan_field_blob")
public class FollowUpPlanFieldBlob extends BaseResourceField {
}
