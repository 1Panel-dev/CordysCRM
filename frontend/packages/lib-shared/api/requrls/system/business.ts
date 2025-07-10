export const GetConfigEmailUrl = '/organization/config/email'; // 获取邮件设置
export const UpdateConfigEmailUrl = '/organization/config/edit/email'; // 更新邮件设置
export const TestConfigEmailUrl = '/organization/config/test/email'; // 邮件设置-测试连接

export const GetConfigSynchronizationUrl = '/organization/config/third'; // 获取三方设置
export const UpdateConfigSynchronizationUrl = '/organization/config/edit/third'; // 更新三方设置
export const TestConfigSynchronizationUrl = '/organization/config/test'; // 三方设置-测试连接
export const GetThirdConfigByTypeUrl = '/organization/config/third/by'; // 根据类型获取开启的三方扫码设置
export const GetThirdTypeListUrl = '/organization/config/third/type/list'; // 获取三方应用扫码类型集合
export const GetDETokenUrl = '/organization/config/de-token'; // 获取DEToken

export const GetAuthsUrl = '/system/authsource/list'; //  认证设置-列表查询
export const GetAuthDetailUrl = '/system/authsource/get'; // 认证设置-详情
export const UpdateAuthUrl = '/system/authsource/update'; // 认证设置-更新
export const CreateAuthUrl = '/system/authsource/add'; // 认证设置-新增
export const UpdateAuthStatusUrl = '/system/authsource/update/status'; // 认证设置-更新状态
export const UpdateAuthNameUrl = '/system/authsource/update/name'; // 认证设置-更新名称
export const DeleteAuthUrl = '/system/authsource/delete'; // 认证设置-删除

// 个人中心
export const GetPersonalUrl = '/personal/center/info';
export const UpdatePersonalUrl = '/personal/center/update';
export const SendEmailCodeUrl = '/personal/center/mail/code/send';
export const UpdateUserPasswordUrl = '/personal/center/info/reset';
export const GetPersonalFollowUrl = '/personal/center/follow/plan/list'; // 用户跟进计划列表

// 查重
export const GetRepeatCustomerUrl = '/personal/center/repeat/customer';
export const GetRepeatContactUrl = '/personal/center/repeat/contact';
export const GetRepeatClueUrl = '/personal/center/repeat/clue';
export const GetRepeatClueDetailUrl = '/personal/center/repeat/clue/detail';
export const GetRepeatOpportunityDetailUrl = '/personal/center/repeat/opportunity/detail';

// 个人中心导出
export const GetExportCenterListUrl = '/export/center/list'; // 查询导出任务列表
export const ExportCenterDownloadUrl = '/export/center/download'; // 下载
export const CancelCenterExportUrl = '/export/center/cancel'; // 取消导出
