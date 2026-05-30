package cn.cordys.crm.approval.constants;

/**
 * 统一的审批状态
 */
public enum ApprovalStatus {

	/** 无 **/
	NONE,
	/** 待提审, 待审批 */
	PENDING,
	/** 审批中 */
	APPROVING,
	/** 已通过 */
	APPROVED,
	/** 已驳回 */
	UNAPPROVED,
	/** 已撤销 */
	REVOKED,

	// 扩展项
	/** 自动通过 */
	AUTO_APPROVED,
	/** 自动拒绝 */
	AUTO_UNAPPROVED
}
