package io.cordys.crm.follow.domain;

import io.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;



@Data
@Table(name = "follow_up_field_blob")
public class FollowUpFieldBlob extends BaseResourceField {
}
