package io.cordys.crm.follow.domain;

import io.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;



@Data
@Table(name = "follow_up_record_field")
public class FollowUpRecordField extends BaseResourceField {
}
