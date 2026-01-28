package cn.cordys.crm.follow.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "follow_up_plan_field_blob")
public class FollowUpPlanFieldBlob extends BaseResourceField {}
