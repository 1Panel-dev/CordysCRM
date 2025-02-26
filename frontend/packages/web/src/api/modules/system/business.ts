import {
  CreateAuthUrl,
  DeleteAuthUrl,
  GetAuthDetailUrl,
  GetAuthsUrl,
  GetConfigEmailUrl,
  GetConfigSynchronizationUrl,
  TestConfigEmailUrl,
  TestConfigSynchronizationUrl,
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
