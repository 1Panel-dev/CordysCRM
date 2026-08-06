export const GetConfigEmailUrl = '/organization/settings/email'; // 获取邮件设置
export const UpdateConfigEmailUrl = '/organization/settings/email/edit'; // 更新邮件设置
export const TestConfigEmailUrl = '/organization/settings/email/test'; // 邮件设置-测试连接

export const GetConfigSynchronizationUrl = '/organization/settings/third-party'; // 获取三方设置
export const UpdateConfigSynchronizationUrl = '/organization/settings/third-party/edit'; // 更新三方设置
export const TestConfigSynchronizationUrl = '/organization/settings/third-party/test'; // 三方设置-测试连接
export const GetThirdTypeListUrl = '/organization/settings/third-party/types'; // 获取三方应用扫码类型集合
export const GetDETokenUrl = '/organization/settings/de-token'; // 获取DEToken
export const SyncDEUrl = '/organization/settings/de/sync'; // 同步 DE 配置
export const GetDEOrgListUrl = '/organization/settings/de/org/list'; // 获取 DE 组织列表
export const GetThirdPartyConfigUrl = '/organization/settings/third-party/get'; // 获取第三方配置
export const SwitchThirdPartyUrl = '/organization/settings/switch-third-party'; // 切换三方平台
export const GetThirdPartyResourceUrl = '/organization/settings/third-party/sync/resource'; // 获取最新的三方同步来源
export const GetAuthsUrl = '/system/auth-sources/list'; //  认证设置-列表查询
export const GetAuthDetailUrl = '/system/auth-sources/get'; // 认证设置-详情
export const UpdateAuthUrl = '/system/auth-sources/update'; // 认证设置-更新
export const CreateAuthUrl = '/system/auth-sources/add'; // 认证设置-新增
export const UpdateAuthStatusUrl = '/system/auth-sources/update/status'; // 认证设置-更新状态
export const UpdateAuthNameUrl = '/system/auth-sources/update/name'; // 认证设置-更新名称
export const DeleteAuthUrl = '/system/auth-sources/delete'; // 认证设置-删除
export const GetTenderConfigUrl = '/tender/application/config'; // 招投标-获取配置项

// 个人中心
export const GetPersonalUrl = '/personal/center/info';
export const UpdatePersonalUrl = '/personal/center/update';
export const SendEmailCodeUrl = '/personal/center/mail/code/send';
export const UpdateUserPasswordUrl = '/personal/center/info/reset';
export const GetPersonalFollowUrl = '/personal/center/follow/plan/list'; // 用户跟进计划列表

// 个人中心导出
export const GetExportCenterListUrl = '/export/center/list'; // 查询导出任务列表
export const ExportCenterDownloadUrl = '/export/center/download'; // 下载
export const CancelCenterExportUrl = '/export/center/cancel'; // 取消导出

// 个人中心ApiKey
export const UpdateApiKeyUrl = '/user/api/key/update'; // 更新 ApiKey
export const GetApiKeyListUrl = '/user/api/key/list'; // 获取 ApiKey 列表
export const EnableApiKeyUrl = '/user/api/key/enable'; // 开启 ApiKey
export const DisableApiKeyUrl = '/user/api/key/disable'; // 关闭 ApiKey
export const DeleteApiKeyUrl = '/user/api/key/delete'; // 删除 ApiKey
export const AddApiKeyUrl = '/user/api/key/add'; // 新增 ApiKey

// 界面设置
export const SavePageConfigUrl = '/ui/display/save'; // 保存界面配置
export const GetPageConfigUrl = '/ui/display/info'; // 获取界面配置
export const GetPageConfigImagePreviewUrl = '/ui/display/preview'; // 图片预览
export const GetTitleImgUrl = `${
  import.meta.env.VITE_API_BASE_URL
}${GetPageConfigImagePreviewUrl}?paramKey=ui.logoPlatform`;

// 模型设置
export const GetAiModelListUrl = '/agent-model/page'; // 模型设置-列表查询
export const GetAiModelOptionsUrl = '/agent-model/options'; // 模型设置-查询可用模型选项
export const AddAiModelUrl = '/agent-model/add'; // 模型设置-添加模型
export const UpdateAiModelUrl = '/agent-model/update'; // 模型设置-更新模型
export const DeleteAiModelUrl = '/agent-model/delete'; // 模型设置-删除模型
export const UpdateAiModelStatusUrl = '/agent-model/switch'; // 模型设置-更新模型状态
export const GetAiModelRouteStrategyUrl = '/organization/settings/model/route-strategy'; // 模型设置-获取路由策略 TODO lmy
export const UpdateAiModelRouteStrategyUrl = '/organization/settings/model/route-strategy/update'; // 模型设置-更新路由策略

// 全局任务
export const AddAgentTaskUrl = '/agent-task/add'; // 全局任务-添加任务
export const UpdateAgentTaskUrl = '/agent-task/update'; // 全局任务-修改任务
export const GetAgentTaskListUrl = '/agent-task/page'; // 全局任务-分页查询任务列表
export const SwitchAgentTaskUrl = '/agent-task/switch'; // 全局任务-启用/禁用任务
export const GetAgentTaskDetailUrl = '/agent-task/get'; // 全局任务-获取任务详情
export const DeleteAgentTaskUrl = '/agent-task/delete'; // 全局任务-删除任务
export const GetAgentTaskExecutionRecordListUrl = '/agent-task/execution-record/page'; // 全局任务-分页查询执行记录
export const StopAgentTaskExecutionRecordUrl = '/agent-task/execution-record/stop'; // 全局任务-停止执行记录
export const DeleteAgentTaskExecutionRecordUrl = '/agent-task/execution-record/delete'; // 全局任务-删除执行记录

// 术语设置
export const GetTermCategoryListUrl = '/agent-term-catalog/list'; // 术语设置-分类列表
export const AddTermCategoryUrl = '/agent-term-catalog/add'; // 术语设置-新增分类
export const UpdateTermCategoryUrl = '/agent-term-catalog/update'; // 术语设置-更新分类
export const DeleteTermCategoryUrl = '/agent-term-catalog/delete'; // 术语设置-删除分类
export const GetTermListUrl = '/agent-term/page'; // 术语设置-分页查询术语列表
export const AddTermUrl = '/agent-term/add'; // 术语设置-新增术语
export const UpdateTermUrl = '/agent-term/update'; // 术语设置-更新术语
export const GetTermDetailUrl = '/agent-term/get'; // 术语设置-术语详情
export const DeleteTermUrl = '/agent-term/delete'; // 术语设置-删除术语
export const SwitchTermUrl = '/agent-term/switch'; // 术语设置-启用/禁用术语
export const DownloadTermTemplateUrl = '/agent-term/template/download'; // 术语设置-下载导入模板
export const ImportTermUrl = '/agent-term/import'; // 术语设置-批量导入术语
export const PreCheckImportTermUrl = '/agent-term/import/pre-check'; // 术语设置-导入预检查
export const GetTermDiscoveryListUrl = '/agent-term-discovery/page'; // 术语设置-分页查询术语发现
export const IgnoreTermDiscoveryUrl = '/agent-term-discovery/ignore'; // 术语设置-忽略术语发现
export const AdoptTermDiscoveryUrl = '/agent-term-discovery/adopt'; // 术语设置-采纳术语发现
