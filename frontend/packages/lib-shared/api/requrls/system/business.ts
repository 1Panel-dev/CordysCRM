export const GetConfigEmailUrl = '/organization/settings/email'; // 获取邮件设置
export const UpdateConfigEmailUrl = '/organization/settings/email/edit'; // 更新邮件设置
export const TestConfigEmailUrl = '/organization/settings/email/test'; // 邮件设置-测试连接

export const GetConfigSynchronizationUrl = '/organization/settings/third-party'; // 获取三方设置
export const UpdateConfigSynchronizationUrl = '/organization/settings/third-party/edit'; // 更新三方设置
export const TestConfigSynchronizationUrl = '/organization/settings/third-party/test'; // 三方设置-测试连接
export const GetThirdConfigByTypeUrl = '/organization/settings/third-party/get'; // 根据类型获取开启的三方扫码设置
export const GetThirdTypeListUrl = '/organization/settings/third-party/types'; // 获取三方应用扫码类型集合
export const GetDETokenUrl = '/organization/settings/de-token'; // 获取DEToken
export const SyncDEUrl = '/organization/settings/de/sync'; // 同步 DE 配置
export const GetDEOrgListUrl = '/organization/settings/de/org/list'; // 获取 DE 组织列表
export const GetThirdPartyConfigUrl = '/organization/settings/third-party/get'; // 获取第三方配置

export const GetAuthsUrl = '/system/auth-sources/list'; //  认证设置-列表查询
export const GetAuthDetailUrl = '/system/auth-sources/get'; // 认证设置-详情
export const UpdateAuthUrl = '/system/auth-sources/update'; // 认证设置-更新
export const CreateAuthUrl = '/system/auth-sources/add'; // 认证设置-新增
export const UpdateAuthStatusUrl = '/system/auth-sources/update/status'; // 认证设置-更新状态
export const UpdateAuthNameUrl = '/system/auth-sources/update/name'; // 认证设置-更新名称
export const DeleteAuthUrl = '/system/auth-sources/delete'; // 认证设置-删除

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
