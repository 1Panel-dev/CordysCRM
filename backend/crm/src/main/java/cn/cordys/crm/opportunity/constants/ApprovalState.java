package cn.cordys.crm.opportunity.constants;

import static cn.cordys.crm.opportunity.constants.OpportunityStageType.AFOOT;
import static cn.cordys.crm.opportunity.constants.OpportunityStageType.END;

import cn.cordys.common.util.Translator;
import lombok.Getter;

@Getter
public enum ApprovalState {

  /** 通过 */
  APPROVED("APPROVED", Translator.get("log.approvalStatus.VOIDED"), END.toString()),

  /** 不通过 */
  UNAPPROVED("UNAPPROVED", Translator.get("log.approvalStatus.UNAPPROVED"), END.toString()),

  /** 提审 */
  APPROVING("APPROVING", Translator.get("log.approvalStatus.APPROVING"), AFOOT.toString()),

  /** 撤销 */
  REVOKED("REVOKED", Translator.get("log.approvalStatus.REVOKED"), AFOOT.toString());

  /** 固定审批节点 ID */
  private final String id;

  /** 审批节点名称 */
  private final String name;

  /** 审批节点类型 */
  private final String type;

  ApprovalState(String id, String name, String type) {
    this.id = id;
    this.name = name;
    this.type = type;
  }
}
