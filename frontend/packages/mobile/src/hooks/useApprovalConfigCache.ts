import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { ApprovalProcessDetail } from '@lib/shared/models/system/process';

import { getApprovalConfigDetail } from '@/api/modules';

type ApprovalConfigKey = FormDesignKeyEnum | string;

const approvalConfigCache = new Map<ApprovalConfigKey, ApprovalProcessDetail | null>();
const approvalConfigPendingMap = new Map<ApprovalConfigKey, Promise<ApprovalProcessDetail | null>>();

export function clearApprovalConfigCache(formKey?: ApprovalConfigKey) {
  if (formKey) {
    approvalConfigCache.delete(formKey);
    approvalConfigPendingMap.delete(formKey);
    return;
  }

  approvalConfigCache.clear();
  approvalConfigPendingMap.clear();
}

export function loadApprovalConfig(formKey: ApprovalConfigKey, forceRefresh = false) {
  if (forceRefresh) {
    approvalConfigCache.delete(formKey);
  }

  if (approvalConfigCache.has(formKey)) {
    return Promise.resolve(approvalConfigCache.get(formKey) ?? null);
  }

  const pendingConfig = approvalConfigPendingMap.get(formKey);
  if (pendingConfig) {
    return pendingConfig;
  }

  const request = getApprovalConfigDetail(formKey)
    .then((result) => {
      const config = result ?? null;
      if (approvalConfigPendingMap.get(formKey) === request) {
        approvalConfigCache.set(formKey, config);
      }
      return config;
    })
    .finally(() => {
      if (approvalConfigPendingMap.get(formKey) === request) {
        approvalConfigPendingMap.delete(formKey);
      }
    });

  approvalConfigPendingMap.set(formKey, request);

  return request;
}
