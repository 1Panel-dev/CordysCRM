import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddApiKeyUrl,
  AddAiModelUrl,
  CancelCenterExportUrl,
  CreateAuthUrl,
  DeleteAiModelUrl,
  DeleteApiKeyUrl,
  DeleteAuthUrl,
  DisableApiKeyUrl,
  EnableApiKeyUrl,
  ExportCenterDownloadUrl,
  GetAiModelListUrl,
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
  GetThirdPartyConfigUrl,
  GetThirdPartyResourceUrl,
  GetThirdTypeListUrl,
  SavePageConfigUrl,
  SendEmailCodeUrl,
  SwitchThirdPartyUrl,
  SyncDEUrl,
  TestConfigEmailUrl,
  TestConfigSynchronizationUrl,
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
  UpdateUserPasswordUrl,
} from '@lib/shared/api/requrls/system/business';
import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
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
  AiModelRouteStrategy,
  AiModelSaveParams,
  AiModelStatusParams,
} from '@lib/shared/models/system/aiModel';
import { type DEToken, OrgUserInfo } from '@lib/shared/models/system/org';

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
    addAiModel,
    updateAiModel,
    updateAiModelStatus,
    deleteAiModel,
    getAiModelRouteStrategy,
    updateAiModelRouteStrategy,
  };
}
