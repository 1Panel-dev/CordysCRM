package cn.cordys.crm.follow.domain;

import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "follow_up_plan_comment")
public class FollowUpPlanComment extends Comment {
}
