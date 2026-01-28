package cn.cordys.crm.follow.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "follow_up_record_field")
public class FollowUpRecordField extends BaseResourceField {}
