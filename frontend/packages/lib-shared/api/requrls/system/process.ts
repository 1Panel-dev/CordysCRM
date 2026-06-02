export const ApprovalPermissionsUrl = '/approval-flow/status-permission/setting'; // 审批流数据权限
export const GetApprovalConfigDetailUrl = '/approval-flow/get-by-form-type'; // 审批流配置详情 用于列表控制操作判断
export const ApprovalProcessPageUrl = '/approval-flow/page'; // 审批流列表
export const AddApprovalProcessUrl = '/approval-flow/add'; // 新增审批流
export const UpdateApprovalProcessUrl = '/approval-flow/update'; // 修改审批流
export const DeleteApprovalProcessUrl = '/approval-flow/delete'; // 删除审批流
export const ApprovalProcessDetailUrl = '/approval-flow/get'; // 审批流详情
export const ToggleApprovalProcessUrl = '/approval-flow/enable'; // 启用｜禁用审批流
export const GetResourceApprovingDetailUrl = '/approval-resource/simple-detail'; // 资源审批状态详情
export const ReviewResourceUrl = '/approval-resource/push'; // 提审
export const RevokeResourceUrl = '/approval-resource/revoke'; // 撤销

// 审批流webHook连接测试
export const TestApprovalWebHookUrl = '/approval-flow/webhook/test '; 

// 审批待办
export const GetProcessedApprovalTodosUrl = '/approval-todo/processed/page'; // 已处理审批待办列表
export const GetPendingApprovalTodosUrl = '/approval-todo/pending/page'; // 待处理审批待办列表
export const GetInitiatedApprovalTodosUrl = '/approval-todo/initiated/page'; // 我发起审批待办列表
export const GetCcApprovalTodosUrl = '/approval-todo/cc/page'; // 抄送我的审批待办列表
export const GetTodoStatisticUrl = '/approval-todo/pending/count'; // 获取待办统计

// 审批
export const RejectApprovalUrl = '/approval-action/reject'; // 驳回
export const BackApprovalUrl = '/approval-action/back'; // 回退
export const AddSignApprovalUrl = '/approval-action/sign'; // 加签
export const RevokeApprovalUrl = '/approval-action/revoke'; // 撤回
export const AgreeApprovalUrl = '/approval-action/approve'; // 同意
export const BatchRejectApprovalUrl = '/approval-action/batch-reject'; // 批量驳回
export const BatchApprovalApprovalUrl = '/approval-action/batch-approve'; // 批量同意

// 审批记录
export const GetApprovalResourceDetailUrl = '/approval-resource/detail'; // 审批资源详情
