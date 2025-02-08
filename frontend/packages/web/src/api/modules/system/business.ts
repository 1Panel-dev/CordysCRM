import CDR from '@/api/http/index';

import {
  GetConfigEmailUrl,
  GetConfigSynchronizationUrl,
  UpdateConfigEmailUrl,
  UpdateConfigSynchronizationUrl,
} from '@lib/shared/api/requrls/system/business';
import type { ConfigEmailParams, ConfigSynchronization } from '@lib/shared/models/system/business';

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
