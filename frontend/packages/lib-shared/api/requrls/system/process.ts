export const ApprovalPermissionsUrl = '/approval-flow/status-permission/setting'; // 审批流数据权限
export const GetApprovalConfigDetailUrl = '/approval-flow/get-by-form-type'; // 审批流配置详情 用于列表控制操作判断
export const ApprovalProcessPageUrl = '/approval-flow/page'; // 审批流列表
export const AddApprovalProcessUrl = '/approval-flow/add'; // 新增审批流
export const UpdateApprovalProcessUrl = '/approval-flow/update'; // 修改审批流
export const DeleteApprovalProcessUrl = '/approval-flow/delete'; // 删除审批流
export const ApprovalProcessDetailUrl = '/approval-flow/get'; // 审批流详情
export const ToggleApprovalProcessUrl = '/approval-flow/enable'; // 启用｜禁用审批流
export const GetResourceApprovingDetailUrl = '/resource-approval/detail'; // 资源审批状态详情
export const ReviewResourceUrl = '/resource-approval/push'; // 提审
export const RevokeResourceUrl = '/resource-approval/pop'; // 撤销 todo xinxinwu

// 审批待办
export const GetProcessedApprovalTodosUrl = '/approval-todo/processed/page'; // 已处理审批待办列表
export const GetPendingApprovalTodosUrl = '/approval-todo/pending/page'; // 待处理审批待办列表
export const GetInitiatedApprovalTodosUrl = '/approval-todo/initiated/page'; // 我发起审批待办列表
export const GetCcApprovalTodosUrl = '/approval-todo/cc/page'; // 抄送我的审批待办列表

// 审批
export const RejectApprovalUrl = '/approval-operation/reject'; // 驳回
export const BackApprovalUrl = '/approval-operation/back'; // 回退
export const AddSignApprovalUrl = '/approval-operation/add'; // 加签

// 审批记录
export const GetApprovalRecordUrl = '/resource-approval/record-detail'; // 审批记录详情
export const GetApprovalResourceDetailUrl = '/resource-approval/detail'; // 审批资源详情
