// 模块首页
export const getModuleNavConfigListUrl = '/module/list'; // 模块-首页-获取模块设置列表
export const moduleNavListSortUrl = '/module/sort'; // 模块-首页-模块排序
export const toggleModuleNavStatusUrl = '/module/switch'; // 模块-首页-单个模块开启或关闭
export const ModuleUserDeptTreeUrl = '/module/user/dept/tree'; // 模块-获取部门用户树
export const ModuleRoleTreeUrl = '/module/role/tree'; // 模块-获取角色树

// 模块--商机
export const getOpportunityListUrl = '/opportunity-rule/page'; // 模块-商机-商机规则列表
export const addOpportunityRuleUrl = '/opportunity-rule/add'; // 模块-商机-添加商机规则
export const updateOpportunityRuleUrl = '/opportunity-rule/update'; // 模块-商机-更新商机规则
export const switchOpportunityStatusUrl = '/opportunity-rule/switch'; // 模块-商机-更新商机规则状态
export const deleteOpportunityUrl = '/opportunity-rule/delete'; // 模块-商机-删除商机规则

// 模块-线索池
export const GetCluePoolPageUrl = '/lead-pool/page'; // 分页获取线索池
export const AddCluePoolUrl = '/lead-pool/add'; // 新增线索池
export const UpdateCluePoolUrl = '/lead-pool/update'; // 编辑线索池
export const QuickUpdateCluePoolUrl = '/lead-pool/quick-update'; // 快捷编辑线索池
export const SwitchCluePoolStatusUrl = '/lead-pool/switch'; // 启用/禁用线索池
export const DeleteCluePoolUrl = '/lead-pool/delete'; // 删除线索池
export const NoPickCluePoolUrl = '/lead-pool/no-pick'; // 未领取线索

// 模块-线索库容
export const GetClueCapacityPageUrl = '/lead-capacity/get'; // 获取线索库容规则
export const AddClueCapacityUrl = '/lead-capacity/add'; // 添加线索库容规则
export const UpdateClueCapacityUrl = '/lead-capacity/update'; // 更新线索库容规则
export const DeleteClueCapacityUrl = '/lead-capacity/delete'; // 删除线索库容规则

// 模块-客户库容
export const GetCustomerCapacityPageUrl = '/account-capacity/get'; // 获取客户库容
export const AddCustomerCapacityUrl = '/account-capacity/add'; // 添加客户库容规则
export const UpdateCustomerCapacityUrl = '/account-capacity/update'; // 更新客户库容规则
export const DeleteCustomerCapacityUrl = '/account-capacity/delete'; // 删除客户库容规则

// 模块-公海池
export const GetCustomerPoolPageUrl = '/account-pool/page'; // 分页获取公海池
export const AddCustomerPoolUrl = '/account-pool/add'; // 新增公海池
export const UpdateCustomerPoolUrl = '/account-pool/update'; // 编辑公海池
export const QuickUpdateCustomerPoolUrl = '/account-pool/quick-update'; // 快捷编辑公海池
export const SwitchCustomerPoolStatusUrl = '/account-pool/switch'; // 启用/禁用公海池
export const DeleteCustomerPoolUrl = '/account-pool/delete'; // 删除公海池
export const NoPickCustomerPoolUrl = '/account-pool/no-pick'; // 未领取线索

// 模块-表单设计
export const GetFormDesignConfigUrl = '/module/form/config'; // 获取表单设计配置
export const SaveFormDesignConfigUrl = '/module/form/save'; // 保存表单设计配置
export const GetFieldDeptUerTreeUrl = '/field/user/dept/tree'; // 获取部门成员树
export const GetFieldDeptTreeUrl = '/field/dept/tree'; // 获取部门树
export const GetFieldProductListUrl = '/field/source/product'; // 获取产品列表
export const GetFieldOpportunityListUrl = '/field/source/opportunity'; // 获取商机列表
export const GetFieldCustomerListUrl = '/field/source/account'; // 获取客户列表
export const GetFieldContactListUrl = '/field/source/contact'; // 获取联系人列表
export const GetFieldClueListUrl = '/field/source/lead'; // 获取线索列表
export const CheckRepeatUrl = '/field/check/repeat'; // 查重

export const UploadTempFileUrl = '/pic/upload/temp'; // 上传临时图片
export const PreviewPictureUrl = '/pic/preview'; // 预览图片
export const DownloadPictureUrl = '/pic/download'; // 下载图片
export const UploadTempAttachmentUrl = '/attachment/upload/temp'; // 上传临时附件
export const PreviewAttachmentUrl = '/attachment/preview'; // 预览附件
export const DownloadAttachmentUrl = '/attachment/download'; // 下载附件
export const DeleteAttachmentUrl = '/attachment/delete'; // 删除附件

// 模块配置-字典管理-原因配置
export const GetReasonUrl = '/dict/get'; // 获取原因
export const AddReasonUrl = '/dict/add'; // 添加原因
export const UpdateReasonUrl = '/dict/update'; // 更新原因
export const DeleteReasonUrl = '/dict/delete'; // 删除原因
export const GetReasonConfigUrl = '/dict/config'; // 获取原因配置
export const UpdateReasonEnableUrl = '/dict/switch'; // 更新原因开关
export const SortReasonUrl = '/dict/sort'; // 原因排序

export const SearchConfigUrl = '/search/config/save'; // 搜索设置添加配置
export const GetSearchConfigUrl = '/search/config/get'; // 获取搜索字段配置
export const ResetSearchConfigUrl = '/search/config/reset'; // 重置搜索字段配置

// 搜索模糊设置
export const ModuleMaskSearchConfigUrl = '/mask/config/save'; // 搜索设置脱敏设置
export const GetModuleMaskSearchConfigUrl = '/mask/config/get'; // 获取搜索脱敏设置

// 系统导航栏
export const GetModuleTopNavListUrl = '/navigation/list'; // 获取顶导配置
export const SetModuleTopNavSortUrl = '/navigation/sort'; // 顶导排序
