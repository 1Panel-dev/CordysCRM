import CDR from '@/api/http/index';

import {
  CreateAuthUrl,
  DeleteAuthUrl,
  GetAuthDetailUrl,
  GetAuthsUrl,
  GetConfigEmailUrl,
  GetConfigSynchronizationUrl,
  UpdateAuthNameUrl,
  UpdateAuthStatusUrl,
  UpdateAuthUrl,
  UpdateConfigEmailUrl,
  UpdateConfigSynchronizationUrl,
} from '@lib/shared/api/requrls/system/business';
import type { CommonList } from '@lib/shared/models/common';
import type {
  Auth,
  AuthItem,
  AuthTableQueryParams,
  AuthUpdateParams,
  ConfigEmailParams,
  ConfigSynchronization,
} from '@lib/shared/models/system/business';

// 获取邮件设置
export function getConfigEmail() {
  return CDR.get<ConfigEmailParams>({ url: GetConfigEmailUrl });
}

// 更新邮件设置
export function updateConfigEmail(data: ConfigEmailParams) {
  return CDR.post({ url: UpdateConfigEmailUrl, data });
}

// 获取同步组织设置
export function getConfigSynchronization() {
  return CDR.get<ConfigSynchronization[]>({ url: GetConfigSynchronizationUrl });
}

// 更新同步组织设置
export function updateConfigSynchronization(data: ConfigSynchronization) {
  return CDR.post({ url: UpdateConfigSynchronizationUrl, data });
}

// 获取认证设置列表
export function getAuthList(data: AuthTableQueryParams) {
  return CDR.post<CommonList<AuthItem>>({ url: GetAuthsUrl, data });
}

// 获取认证设置详情
export function getAuthDetail(id: string) {
  return CDR.post<AuthUpdateParams>({ url: `${GetAuthDetailUrl}/${id}` });
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
  return CDR.post({ url: `${UpdateAuthStatusUrl}/${id}`, data: { enable } });
}

// 更新认证设置名称
export function updateAuthName(id: string, name: string) {
  return CDR.post({ url: `${UpdateAuthNameUrl}/${id}`, data: { name } });
}

// 删除认证设置
export function deleteAuth(id: string) {
  return CDR.post({ url: `${DeleteAuthUrl}/${id}` });
}
