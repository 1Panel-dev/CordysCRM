// 线索
export const GetClueFormConfigUrl = '/lead/module/form'; // 获取线索表单配置
export const UpdateClueUrl = '/lead/update'; // 更新线索
export const UpdateClueStatusUrl = '/lead/status/update'; // 更新线索状态
export const GetClueListUrl = '/lead/page'; // 分页查询线索
export const GetClueTransitionCustomerListUrl = '/lead/transition/account/page'; // 线索转为客户列表
export const AddClueUrl = '/lead/add'; // 添加线索
export const GetClueUrl = '/lead/get'; // 获取线索详情
export const DeleteClueUrl = '/lead/delete'; // 删除线索
export const BatchTransferClueUrl = '/lead/batch/transfer'; // 批量转移线索
export const BatchToPoolClueUrl = '/lead/batch/to-pool'; // 批量移入线索池
export const BatchDeleteClueUrl = '/lead/batch/delete'; // 批量删除线索
export const ExportClueAllUrl = '/lead/export'; // 导出全部线索
export const ExportClueSelectedUrl = '/lead/export-select'; // 导出选中线索
export const ReTransitionCustomerUrl = '/lead/re-transition/account'; // 合并线索转为客户
export const MoveToPoolLeadUrl = '/lead/to-pool'; // 移入线索池
export const TransformClueUrl = '/lead/transform'; // 转换线索
export const GetAdvancedSearchClueListUrl = '/advanced/search/lead'; // 全局搜索线索分页查询线索
export const GetAdvancedSearchClueDetailUrl = '/advanced/search/lead/detail'; // 全局搜索线索详情
export const GetGlobalSearchClueListUrl = '/global/search/lead';
export const GetGlobalCluePoolListUrl = '/global/search/clue_pool';

// 跟进记录
export const UpdateClueFollowRecordUrl = '/lead/follow/record/update'; // 更新跟进记录
export const GetClueFollowRecordListUrl = '/lead/follow/record/page'; // 获取跟进记录列表
export const AddClueFollowRecordUrl = '/lead/follow/record/add'; // 添加跟进记录
export const GetClueFollowRecordUrl = '/lead/follow/record/get'; // 获取跟进记录详情
export const DeleteClueFollowRecordUrl = '/lead/follow/record/delete'; // 删除跟进记录

// 跟进计划
export const UpdateClueFollowPlanUrl = '/lead/follow/plan/update'; // 更新跟进计划
export const GetClueFollowPlanListUrl = '/lead/follow/plan/page'; // 获取跟进计划列表
export const AddClueFollowPlanUrl = '/lead/follow/plan/add'; // 添加跟进计划
export const GetClueFollowPlanUrl = '/lead/follow/plan/get'; // 跟进计划详情
export const CancelClueFollowPlanUrl = '/lead/follow/plan/cancel'; // 取消跟进计划
export const DeleteClueFollowPlanUrl = '/lead/follow/plan/delete'; // 删除跟进计划
export const UpdateClueFollowPlanStatusUrl = '/lead/follow/plan/status/update'; // 更新线索跟进计划状态

export const GetClueHeaderListUrl = '/lead/owner/history/list'; // 线索负责人记录列表

// 线索池客户
export const PickClueUrl = '/pool/lead/pick'; // 领取线索
export const GetCluePoolListUrl = '/pool/lead/page'; // 分页查询线索池线索
export const BatchPickClueUrl = '/pool/lead/batch-pick'; // 批量领取线索
export const BatchDeleteCluePoolUrl = '/pool/lead/batch-delete'; // 批量删除线索池线索
export const BatchAssignClueUrl = '/pool/lead/batch-assign'; // 批量分配线索
export const AssignClueUrl = '/pool/lead/assign'; // 分配线索
export const GetPoolOptionsUrl = '/pool/lead/options'; // 获取当前用户线索池选项
export const DeleteCluePoolUrl = '/pool/lead/delete'; // 删除线索池线索
export const GetPoolClueUrl = '/pool/lead/get'; // 获取线索池详情
export const ClueTransitionCustomerUrl = '/lead/transition/account'; // 转为客户
export const GetAdvancedCluePoolListUrl = '/advanced/search/lead-pool'; // 全局搜索分页查询线索池线索
export const ExportCluePoolAllUrl = '/pool/lead/export-all'; // 导出全部线索池线索
export const ExportCluePoolSelectedUrl = '/pool/lead/export-select'; // 导出选中线索池线索
export const BatchUpdateCluePoolUrl = '/pool/lead/batch-update'; // 批量更新线索池线索

// 线索池跟进记录
export const GetCluePoolFollowRecordListUrl = '/lead/follow/record/pool/page'; // 获取跟进记录列表

export const GetClueTabUrl = '/lead/tab'; // 线索tab显隐

// 视图
export const GetClueViewDetailUrl = '/lead/view/detail';
export const GetClueViewListUrl = '/lead/view/list';
export const AddClueViewUrl = '/lead/view/add';
export const UpdateClueViewUrl = '/lead/view/update';
export const DeleteClueViewUrl = '/lead/view/delete';
export const FixedClueViewUrl = '/lead/view/fixed';
export const EnableClueViewUrl = '/lead/view/enable';
export const DragClueViewUrl = '/lead/view/edit/pos';

// 导入
export const PreCheckImportUrl = '/lead/import/pre-check';
export const DownloadTemplateUrl = '/lead/template/download';
export const ImportLeadUrl = '/lead/import';
