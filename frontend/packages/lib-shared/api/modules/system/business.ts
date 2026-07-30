import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddApiKeyUrl,
  AddAiModelUrl,
  AddAgentTaskUrl,
  AddTermCategoryUrl,
  AddTermUrl,
  AdoptTermDiscoveryUrl,
  CancelCenterExportUrl,
  CreateAuthUrl,
  DeleteAgentTaskUrl,
  DeleteAiModelUrl,
  DeleteApiKeyUrl,
  DeleteAuthUrl,
  DeleteTermCategoryUrl,
  DeleteTermUrl,
  DisableApiKeyUrl,
  DownloadTermTemplateUrl,
  EnableApiKeyUrl,
  ExportCenterDownloadUrl,
  GetAgentTaskDetailUrl,
  GetAgentTaskExecutionRecordListUrl,
  GetAgentTaskListUrl,
  GetAiModelListUrl,
  GetAiModelOptionsUrl,
  GetAiModelRouteStrategyUrl,
  GetApiKeyListUrl,
  GetAuthDetailUrl,
  GetAuthsUrl,
  GetConfigEmailUrl,
  GetConfigSynchronizationUrl,
  GetDEOrgListUrl,
  GetDETokenUrl,
  GetExportCenterListUrl,
  GetPageConfigUrl,
  GetPersonalFollowUrl,
  GetPersonalUrl,
  GetTenderConfigUrl,
  GetTermCategoryListUrl,
  GetTermDetailUrl,
  GetTermDiscoveryListUrl,
  GetTermListUrl,
  GetThirdPartyConfigUrl,
  GetThirdPartyResourceUrl,
  GetThirdTypeListUrl,
  ImportTermUrl,
  IgnoreTermDiscoveryUrl,
  PreCheckImportTermUrl,
  SavePageConfigUrl,
  SendEmailCodeUrl,
  SwitchAgentTaskUrl,
  SwitchTermUrl,
  SwitchThirdPartyUrl,
  SyncDEUrl,
  TestConfigEmailUrl,
  TestConfigSynchronizationUrl,
  UpdateAgentTaskUrl,
  UpdateAiModelRouteStrategyUrl,
  UpdateAiModelStatusUrl,
  UpdateAiModelUrl,
  UpdateApiKeyUrl,
  UpdateAuthNameUrl,
  UpdateAuthStatusUrl,
  UpdateAuthUrl,
  UpdateConfigEmailUrl,
  UpdateConfigSynchronizationUrl,
  UpdatePersonalUrl,
  UpdateTermCategoryUrl,
  UpdateTermUrl,
  UpdateUserPasswordUrl,
} from '@lib/shared/api/requrls/system/business';
import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import type { CommonList, ImportUploadParams, TableQueryParams } from '@lib/shared/models/common';
import { CustomerFollowPlanTableParams, FollowDetailItem } from '@lib/shared/models/customer';
import type {
  ApiKey,
  Auth,
  AuthItem,
  AuthTableQueryParams,
  AuthUpdateParams,
  ConfigEmailParams,
  ThirdPartyResourceConfig,
  DEOrgItem,
  PageConfigReturns,
  SavePageConfigParams,
  ThirdPartyResource,
  UpdateApiKeyParams,
  ThirdPartyDEConfig,
} from '@lib/shared/models/system/business';
import {
  ExportCenterItem,
  ExportCenterListParams,
  OptionDTO,
  PersonalInfoRequest,
  PersonalPassword,
  SendEmailDTO,
} from '@lib/shared/models/system/business';
import type {
  AiModelItem,
  AiModelOption,
  AiModelRouteStrategy,
  AiModelSaveParams,
  AiModelStatusParams,
} from '@lib/shared/models/system/aiModel';
import type { AgentTaskExecutionRecordItem, AgentTaskItem, AgentTaskParams } from '@lib/shared/models/system/agentTask';
import { type DEToken, OrgUserInfo, type ValidateInfo } from '@lib/shared/models/system/org';
import type {
  TermCategoryItem,
  TermCategoryParams,
  TermDiscoveryAdoptParams,
  TermDiscoveryItem,
  TermItem,
  TermListParams,
  TermParams,
} from '@lib/shared/models/system/term';

export default function useProductApi(CDR: CordysAxios) {
  // 获取邮件设置
  function getConfigEmail() {
    return CDR.get<ConfigEmailParams>({ url: GetConfigEmailUrl });
  }

  // 更新邮件设置
  function updateConfigEmail(data: ConfigEmailParams) {
    return CDR.post({ url: UpdateConfigEmailUrl, data });
  }

  // 邮件设置-测试连接
  function testConfigEmail(data: ConfigEmailParams) {
    return CDR.post({ url: TestConfigEmailUrl, data });
  }

  // 同步组织设置-测试连接
  function testConfigSynchronization(data: ThirdPartyResourceConfig) {
    return CDR.post({ url: TestConfigSynchronizationUrl, data }, { isReturnNativeResponse: true });
  }

  // 获取同步组织设置
  function getConfigSynchronization() {
    return CDR.get<ThirdPartyResourceConfig[]>({ url: GetConfigSynchronizationUrl }, { ignoreCancelToken: true });
  }

  // 更新同步组织设置
  function updateConfigSynchronization(data: ThirdPartyResourceConfig) {
    return CDR.post({ url: UpdateConfigSynchronizationUrl, data }, { isReturnNativeResponse: true });
  }

  // 根据类型获取开启的三方扫码设置
  function getThirdConfigByType<T = ThirdPartyResourceConfig>(type: string, isReturnNativeResponse = false) {
    return CDR.get<T>(
      { url: `${GetThirdPartyConfigUrl}/${type}` },
      {
        noErrorTip: true,
        isReturnNativeResponse,
      }
    );
  }

  // 获取三方应用扫码类型集合
  function getThirdTypeList() {
    return CDR.get<OptionDTO[]>({ url: GetThirdTypeListUrl });
  }

  // 切换三方平台
  function switchThirdParty(type: CompanyTypeEnum) {
    return CDR.get({ url: SwitchThirdPartyUrl, params: { type } });
  }

  // 获取最新的三方同步来源
  function getThirdPartyResource() {
    return CDR.get<ThirdPartyResource>(
      { url: GetThirdPartyResourceUrl },
      {
        ignoreCancelToken: true,
      }
    );
  }

  // 获取认证设置列表
  function getAuthList(data: AuthTableQueryParams) {
    return CDR.post<CommonList<AuthItem>>({ url: GetAuthsUrl, data });
  }

  // 获取认证设置详情
  function getAuthDetail(id: string) {
    return CDR.get<AuthUpdateParams>({ url: `${GetAuthDetailUrl}/${id}` });
  }

  // 更新认证设置
  function updateAuth(data: AuthUpdateParams) {
    return CDR.post({ url: UpdateAuthUrl, data });
  }

  // 新建认证设置
  function createAuth(data: Auth) {
    return CDR.post({ url: CreateAuthUrl, data });
  }

  // 更新认证设置状态
  function updateAuthStatus(id: string, enable: boolean) {
    return CDR.get({ url: `${UpdateAuthStatusUrl}/${id}`, params: { enable } });
  }

  // 更新认证设置名称
  function updateAuthName(id: string, name: string) {
    return CDR.get({ url: `${UpdateAuthNameUrl}/${id}`, params: { name } });
  }

  // 删除认证设置
  function deleteAuth(id: string) {
    return CDR.get({ url: `${DeleteAuthUrl}/${id}` });
  }

  // 获取DEToken
  function getDEToken(isModule = false) {
    return CDR.get<DEToken>({ url: GetDETokenUrl, params: { isModule } });
  }

  // 同步 DE
  function syncDE() {
    return CDR.get({ url: SyncDEUrl });
  }

  // 获取第三方配置
  function getThirdPartyConfig(type: string) {
    return CDR.get<ThirdPartyResourceConfig>({ url: `${GetThirdPartyConfigUrl}/${type}` }, { noErrorTip: true });
  }

  // 获取 DE 组织列表
  function getDEOrgList(data: ThirdPartyDEConfig) {
    return CDR.post<DEOrgItem[]>({ url: GetDEOrgListUrl, data });
  }

  // 获取个人信息
  function getPersonalInfo() {
    return CDR.get<OrgUserInfo>({ url: GetPersonalUrl });
  }
  // 更新个人信息
  function updatePersonalInfo(data: PersonalInfoRequest) {
    return CDR.post({ url: UpdatePersonalUrl, data });
  }
  // 发送验证码
  function sendEmailCode(email: SendEmailDTO) {
    return CDR.post({ url: SendEmailCodeUrl, params: { email } });
  }
  // 修改密码
  function updateUserPassword(data: PersonalPassword) {
    return CDR.post({ url: UpdateUserPasswordUrl, data });
  }

  // 获取个人跟进计划
  function getPersonalFollow(data: CustomerFollowPlanTableParams) {
    return CDR.post<CommonList<FollowDetailItem>>({ url: GetPersonalFollowUrl, data });
  }

  //  个人中心导出列表
  function getExportCenterList(data: ExportCenterListParams) {
    return CDR.post<ExportCenterItem[]>({ url: GetExportCenterListUrl, data });
  }

  //  个人中心导出下载
  function exportCenterDownload(taskId: string) {
    return CDR.get(
      { url: `${ExportCenterDownloadUrl}/${taskId}`, responseType: 'blob' },
      { isTransformResponse: false }
    );
  }

  //  个人中心取消导出
  function cancelCenterExport(taskId: string) {
    return CDR.get({ url: `${CancelCenterExportUrl}/${taskId}` });
  }

  // 个人中心 ApiKey
  // 更新ApiKey
  function updateApiKey(data: UpdateApiKeyParams) {
    return CDR.post({ url: UpdateApiKeyUrl, data });
  }

  // 获取ApiKey列表
  function getApiKeyList() {
    return CDR.get<ApiKey[]>({ url: GetApiKeyListUrl });
  }

  // 开启ApiKey
  function enableApiKey(id: string) {
    return CDR.get({ url: EnableApiKeyUrl, params: id });
  }

  // 关闭ApiKey
  function disableApiKey(id: string) {
    return CDR.get({ url: DisableApiKeyUrl, params: id });
  }

  // 删除ApiKey
  function deleteApiKey(id: string) {
    return CDR.get({ url: DeleteApiKeyUrl, params: id });
  }

  // 新增ApiKey
  function addApiKey() {
    return CDR.get({ url: AddApiKeyUrl });
  }

  // 保存界面配置
  function savePageConfig(data: SavePageConfigParams) {
    return CDR.uploadFile({ url: SavePageConfigUrl }, data, 'files');
  }

  // 获取界面配置
  function getPageConfig() {
    return CDR.get<PageConfigReturns>({ url: GetPageConfigUrl }, { ignoreCancelToken: true });
  }

  // 获取招投标配置项
  function getTenderConfig() {
    return CDR.get<ThirdPartyResourceConfig>({ url: GetTenderConfigUrl }, { ignoreCancelToken: true });
  }

  // 模型设置-列表查询
  function getAiModelList(data: TableQueryParams) {
    return CDR.post<CommonList<AiModelItem>>({ url: GetAiModelListUrl, data });
  }

  // 模型设置-查询可用模型选项
  function getAiModelOptions() {
    return CDR.get<AiModelOption[]>({ url: GetAiModelOptionsUrl });
  }

  // 模型设置-添加模型
  function addAiModel(data: AiModelSaveParams) {
    return CDR.post({ url: AddAiModelUrl, data });
  }

  // 模型设置-更新模型
  function updateAiModel(data: AiModelSaveParams) {
    return CDR.post({ url: UpdateAiModelUrl, data });
  }

  // 模型设置-更新模型状态
  function updateAiModelStatus(data: AiModelStatusParams) {
    return CDR.get({ url: `${UpdateAiModelStatusUrl}/${data.id}` });
  }

  // 模型设置-删除模型
  function deleteAiModel(id: string) {
    return CDR.get({ url: `${DeleteAiModelUrl}/${id}` });
  }

  // 模型设置-获取路由策略
  function getAiModelRouteStrategy() {
    return CDR.get<AiModelRouteStrategy>({ url: GetAiModelRouteStrategyUrl });
  }

  // 模型设置-更新路由策略
  function updateAiModelRouteStrategy(data: AiModelRouteStrategy) {
    return CDR.post({ url: UpdateAiModelRouteStrategyUrl, data });
  }

  // 全局任务-分页查询任务列表
  function getAgentTaskList(data: TableQueryParams) {
    return CDR.post<CommonList<AgentTaskItem>>({ url: GetAgentTaskListUrl, data });
  }

  // 全局任务-添加任务
  function addAgentTask(data: AgentTaskParams) {
    return CDR.post({ url: AddAgentTaskUrl, data });
  }

  // 全局任务-修改任务
  function updateAgentTask(data: AgentTaskParams) {
    return CDR.post({ url: UpdateAgentTaskUrl, data });
  }

  // 全局任务-启用/禁用任务
  function switchAgentTask(id: string) {
    return CDR.get({ url: `${SwitchAgentTaskUrl}/${id}` });
  }

  // 全局任务-获取任务详情
  function getAgentTaskDetail(id: string) {
    return CDR.get<AgentTaskItem>({ url: `${GetAgentTaskDetailUrl}/${id}` });
  }

  // 全局任务-删除任务
  function deleteAgentTask(id: string) {
    return CDR.get({ url: `${DeleteAgentTaskUrl}/${id}` });
  }

  // 全局任务-分页查询执行记录
  function getAgentTaskExecutionRecordList(data: TableQueryParams) {
    return CDR.post<CommonList<AgentTaskExecutionRecordItem>>({ url: GetAgentTaskExecutionRecordListUrl, data });
  }

  // 术语设置-分类列表
  function getTermCategoryList() {
    return CDR.get<TermCategoryItem[]>({ url: GetTermCategoryListUrl });
  }

  // 术语设置-新增分类
  function addTermCategory(data: TermCategoryParams) {
    return CDR.post<TermCategoryItem>({ url: AddTermCategoryUrl, data });
  }

  // 术语设置-更新分类
  function updateTermCategory(data: TermCategoryParams) {
    return CDR.post({ url: UpdateTermCategoryUrl, data });
  }

  // 术语设置-删除分类
  function deleteTermCategory(id: string) {
    return CDR.get({ url: `${DeleteTermCategoryUrl}/${id}` });
  }

  // 术语设置-分页查询术语列表
  function getTermList(data: TermListParams) {
    return CDR.post<CommonList<TermItem>>({ url: GetTermListUrl, data });
  }

  // 术语设置-新增术语
  function addTerm(data: TermParams) {
    return CDR.post<TermItem>({ url: AddTermUrl, data });
  }

  // 术语设置-更新术语
  function updateTerm(data: TermParams) {
    return CDR.post({ url: UpdateTermUrl, data });
  }

  // 术语设置-术语详情
  function getTermDetail(id: string) {
    return CDR.get<TermItem>({ url: `${GetTermDetailUrl}/${id}` });
  }

  // 术语设置-删除术语
  function deleteTerm(id: string) {
    return CDR.get({ url: `${DeleteTermUrl}/${id}` });
  }

  // 术语设置-启用/禁用术语
  function switchTerm(id: string) {
    return CDR.get({ url: `${SwitchTermUrl}/${id}` });
  }

  // 术语设置-导入预检查
  function preCheckImportTerm(params: ImportUploadParams) {
    return CDR.uploadFile<{ data: ValidateInfo }>({ url: PreCheckImportTermUrl }, params, 'file');
  }

  // 术语设置-下载导入模板
  function downloadTermTemplate() {
    return CDR.get(
      {
        url: DownloadTermTemplateUrl,
        responseType: 'blob',
      },
      { isTransformResponse: false, isReturnNativeResponse: true }
    );
  }

  // 术语设置-批量导入术语
  function importTerm(params: ImportUploadParams) {
    return CDR.uploadFile({ url: ImportTermUrl }, params, 'file');
  }

  // 术语设置-分页查询术语发现
  function getTermDiscoveryList(data: TableQueryParams) {
    return CDR.post<CommonList<TermDiscoveryItem>>({ url: GetTermDiscoveryListUrl, data });
  }

  // 术语设置-忽略术语发现
  function ignoreTermDiscovery(id: string) {
    return CDR.get({ url: `${IgnoreTermDiscoveryUrl}/${id}` });
  }

  // 术语设置-采纳术语发现
  function adoptTermDiscovery(data: TermDiscoveryAdoptParams) {
    return CDR.post<TermItem>({ url: AdoptTermDiscoveryUrl, data });
  }

  return {
    getConfigEmail,
    updateConfigEmail,
    testConfigEmail,
    testConfigSynchronization,
    getConfigSynchronization,
    updateConfigSynchronization,
    getThirdConfigByType,
    getThirdTypeList,
    getAuthList,
    getAuthDetail,
    updateAuth,
    createAuth,
    updateAuthStatus,
    updateAuthName,
    deleteAuth,
    switchThirdParty,
    getThirdPartyResource,
    getPersonalInfo,
    updatePersonalInfo,
    sendEmailCode,
    updateUserPassword,
    getPersonalFollow,
    getExportCenterList,
    exportCenterDownload,
    cancelCenterExport,
    getDEToken,
    syncDE,
    getDEOrgList,
    getThirdPartyConfig,
    updateApiKey,
    getApiKeyList,
    enableApiKey,
    disableApiKey,
    deleteApiKey,
    addApiKey,
    savePageConfig,
    getPageConfig,
    getTenderConfig,
    getAiModelList,
    getAiModelOptions,
    addAiModel,
    updateAiModel,
    updateAiModelStatus,
    deleteAiModel,
    getAiModelRouteStrategy,
    updateAiModelRouteStrategy,
    getAgentTaskList,
    getAgentTaskExecutionRecordList,
    addAgentTask,
    updateAgentTask,
    switchAgentTask,
    getAgentTaskDetail,
    deleteAgentTask,
    getTermCategoryList,
    addTermCategory,
    updateTermCategory,
    deleteTermCategory,
    getTermList,
    addTerm,
    updateTerm,
    getTermDetail,
    deleteTerm,
    switchTerm,
    preCheckImportTerm,
    downloadTermTemplate,
    importTerm,
    getTermDiscoveryList,
    ignoreTermDiscovery,
    adoptTermDiscovery,
  };
}
