import {
  CreateAuthUrl,
  DeleteAuthUrl,
  GetAuthDetailUrl,
  GetAuthsUrl,
  GetConfigEmailUrl,
  GetConfigSynchronizationUrl,
  GetPersonalFollowUrl,
  GetPersonalUrl,
  GetRepeatCustomerUrl,
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
} from '@lib/shared/models/system/business';
import { OptionDTO, PersonalInfoRequest, PersonalPassword } from '@lib/shared/models/system/business';
import { OrgUserInfo } from '@lib/shared/models/system/org';

import CDR from '@/api/http/index';

// 获取邮件设置
export function getConfigEmail() {
  return CDR.get<ConfigEmailParams>({ url: GetConfigEmailUrl });
}

// 更新邮件设置
export function updateConfigEmail(data: ConfigEmailParams) {
  return CDR.post({ url: UpdateConfigEmailUrl, data });
}

// 邮件设置-测试连接
export function testConfigEmail(data: ConfigEmailParams) {
  return CDR.post({ url: TestConfigEmailUrl, data });
}

// 同步组织设置-测试连接
export function testConfigSynchronization(data: ConfigSynchronization) {
  return CDR.post({ url: TestConfigSynchronizationUrl, data });
}

// 获取同步组织设置
export function getConfigSynchronization() {
  return CDR.get<ConfigSynchronization[]>({ url: GetConfigSynchronizationUrl });
}

// 更新同步组织设置
export function updateConfigSynchronization(data: ConfigSynchronization) {
  return CDR.post({ url: UpdateConfigSynchronizationUrl, data });
}

// 根据类型获取开启的三方扫码设置
export function getThirdConfigByType(type: string) {
  return CDR.get<ConfigSynchronization>({ url: `${GetThirdConfigByTypeUrl}/${type}` });
}

// 获取三方应用扫码类型集合
export function getThirdTypeList() {
  return CDR.get<OptionDTO[]>({ url: GetThirdTypeListUrl });
}

// 获取认证设置列表
export function getAuthList(data: AuthTableQueryParams) {
  return CDR.post<CommonList<AuthItem>>({ url: GetAuthsUrl, data });
}

// 获取认证设置详情
export function getAuthDetail(id: string) {
  return CDR.get<AuthUpdateParams>({ url: `${GetAuthDetailUrl}/${id}` });
}

// 更新认证设置
export function updateAuth(data: AuthUpdateParams) {
  return CDR.post({ url: UpdateAuthUrl, data });
}

// 新建认证设置
export function createAuth(data: Auth) {
  return CDR.post({ url: CreateAuthUrl, data });
}

// 更新认证设置状态
export function updateAuthStatus(id: string, enable: boolean) {
  return CDR.get({ url: `${UpdateAuthStatusUrl}/${id}`, params: { enable } });
}

// 更新认证设置名称
export function updateAuthName(id: string, name: string) {
  return CDR.get({ url: `${UpdateAuthNameUrl}/${id}`, params: { name } });
}

// 删除认证设置
export function deleteAuth(id: string) {
  return CDR.get({ url: `${DeleteAuthUrl}/${id}` });
}

// 获取个人信息
export function getPersonalUrl() {
  return CDR.get<OrgUserInfo>({ url: GetPersonalUrl });
}
// 更新个人信息
export function updatePersonalUrl(data: PersonalInfoRequest) {
  return CDR.post({ url: UpdatePersonalUrl, data });
}
// 发送验证码
export function sendEmailCode(email: string) {
  return CDR.post({ url: SendEmailCodeUrl, params: { email } });
}
// 修改密码
export function updateUserPassword(data: PersonalPassword) {
  return CDR.post({ url: UpdateUserPasswordUrl, data });
}

// 获取个人跟进计划
export function getPersonalFollow(data: CustomerFollowPlanTableParams) {
  return CDR.post<CommonList<FollowDetailItem>>({ url: GetPersonalFollowUrl, data });
}

// 查重 TODO lmy 联调
export function GetRepeatCustomerData(data: any) {
  return CDR.post<CommonList<any>>({ url: GetRepeatCustomerUrl, data });
}
