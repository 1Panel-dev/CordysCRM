import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  CancelCenterExportUrl,
  CreateAuthUrl,
  DeleteAuthUrl,
  ExportCenterDownloadUrl,
  GetAuthDetailUrl,
  GetAuthsUrl,
  GetConfigEmailUrl,
  GetConfigSynchronizationUrl,
  GetDETokenUrl,
  GetExportCenterListUrl,
  GetPersonalFollowUrl,
  GetPersonalUrl,
  GetRepeatClueDetailUrl,
  GetRepeatClueUrl,
  GetRepeatContactUrl,
  GetRepeatCustomerUrl,
  GetRepeatOpportunityDetailUrl,
  GetThirdConfigByTypeUrl,
  GetThirdTypeListUrl,
  SendEmailCodeUrl,
  TestConfigEmailUrl,
  TestConfigSynchronizationUrl,
  UpdateAuthNameUrl,
  UpdateAuthStatusUrl,
  UpdateAuthUrl,
  UpdateConfigEmailUrl,
  UpdateConfigSynchronizationUrl,
  UpdatePersonalUrl,
  UpdateUserPasswordUrl,
} from '@lib/shared/api/requrls/system/business';
import type { CommonList } from '@lib/shared/models/common';
import { CustomerFollowPlanTableParams, FollowDetailItem } from '@lib/shared/models/customer';
import type {
  Auth,
  AuthItem,
  AuthTableQueryParams,
  AuthUpdateParams,
  ConfigEmailParams,
  ConfigSynchronization,
  RepeatClueItem,
  RepeatClueParams,
  RepeatContactItem,
  RepeatCustomerItem,
  RepeatOpportunityItem,
} from '@lib/shared/models/system/business';
import {
  ExportCenterItem,
  ExportCenterListParams,
  OptionDTO,
  PersonalInfoRequest,
  PersonalPassword,
  SendEmailDTO,
} from '@lib/shared/models/system/business';
import { OrgUserInfo } from '@lib/shared/models/system/org';

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
  function testConfigSynchronization(data: ConfigSynchronization) {
    return CDR.post({ url: TestConfigSynchronizationUrl, data }, { isReturnNativeResponse: true });
  }

  // 获取同步组织设置
  function getConfigSynchronization() {
    return CDR.get<ConfigSynchronization[]>({ url: GetConfigSynchronizationUrl });
  }

  // 更新同步组织设置
  function updateConfigSynchronization(data: ConfigSynchronization) {
    return CDR.post({ url: UpdateConfigSynchronizationUrl, data }, { isReturnNativeResponse: true });
  }

  // 根据类型获取开启的三方扫码设置
  function getThirdConfigByType(type: string) {
    return CDR.get<ConfigSynchronization>({ url: `${GetThirdConfigByTypeUrl}/${type}` });
  }

  // 获取三方应用扫码类型集合
  function getThirdTypeList() {
    return CDR.get<OptionDTO[]>({ url: GetThirdTypeListUrl });
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
  function getDEToken() {
    return CDR.get({ url: GetDETokenUrl });
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

  // 查重客户相关
  function GetRepeatCustomerList(data: RepeatClueParams) {
    return CDR.post<CommonList<RepeatCustomerItem>>(
      { url: GetRepeatCustomerUrl, data },
      { isReturnNativeResponse: true }
    );
  }

  // 查重联系人相关
  function getRepeatContactList(data: RepeatClueParams) {
    return CDR.post<CommonList<RepeatContactItem>>(
      { url: GetRepeatContactUrl, data },
      { isReturnNativeResponse: true }
    );
  }

  function GetRepeatClueList(data: RepeatClueParams) {
    return CDR.post<CommonList<RepeatCustomerItem>>({ url: GetRepeatClueUrl, data }, { isReturnNativeResponse: true });
  }

  function GetRepeatClueDetailList(data: RepeatClueParams) {
    return CDR.post<CommonList<RepeatClueItem>>({ url: GetRepeatClueDetailUrl, data });
  }

  function GetRepeatOpportunityDetailList(data: RepeatClueParams) {
    return CDR.post<CommonList<RepeatOpportunityItem>>({ url: GetRepeatOpportunityDetailUrl, data });
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
    getPersonalInfo,
    updatePersonalInfo,
    sendEmailCode,
    updateUserPassword,
    getPersonalFollow,
    GetRepeatCustomerList,
    GetRepeatClueList,
    GetRepeatClueDetailList,
    GetRepeatOpportunityDetailList,
    getExportCenterList,
    exportCenterDownload,
    cancelCenterExport,
    getRepeatContactList,
    getDEToken,
  };
}
