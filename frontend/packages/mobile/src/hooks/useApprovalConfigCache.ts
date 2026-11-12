import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { ApprovalProcessDetail } from '@lib/shared/models/system/process';

import { getApprovalConfigDetail } from '@/api/modules';

type ApprovalConfigKey = FormDesignKeyEnum | string;

const approvalConfigCache = new Map<ApprovalConfigKey, ApprovalProcessDetail | null>();
const approvalConfigPendingMap = new Map<ApprovalConfigKey, Promise<ApprovalProcessDetail | null>>();

export function clearApprovalConfigCache(formKey?: ApprovalConfigKey) {
  if (formKey) {
    let formKeys = [formKey];
    if (formKey === FormDesignKeyEnum.CLUE || formKey === FormDesignKeyEnum.CLUE_POOL) {
      formKeys = [FormDesignKeyEnum.CLUE, FormDesignKeyEnum.CLUE_POOL];
    } else if (formKey === FormDesignKeyEnum.CUSTOMER || formKey === FormDesignKeyEnum.CUSTOMER_OPEN_SEA) {
      formKeys = [FormDesignKeyEnum.CUSTOMER, FormDesignKeyEnum.CUSTOMER_OPEN_SEA];
    }
    [...approvalConfigCache.keys(), ...approvalConfigPendingMap.keys()].forEach((key) => {
      if (formKeys.includes(JSON.parse(key)[0])) {
        approvalConfigCache.delete(key);
        approvalConfigPendingMap.delete(key);
      }
    });
    return;
  }

  approvalConfigCache.clear();
  approvalConfigPendingMap.clear();
}

export function loadApprovalConfig(formKey: ApprovalConfigKey, forceRefresh = false, poolId?: string | number) {
  const cacheKey = JSON.stringify([formKey, poolId ? String(poolId) : null]);
  if (forceRefresh) {
    approvalConfigCache.delete(cacheKey);
  }

  if (approvalConfigCache.has(cacheKey)) {
    return Promise.resolve(approvalConfigCache.get(cacheKey) ?? null);
  }

  const pendingConfig = approvalConfigPendingMap.get(cacheKey);
  if (pendingConfig) {
    return pendingConfig;
  }

  const request = getApprovalConfigDetail(formKey, poolId)
    .then((result) => {
      const config = result ?? null;
      if (approvalConfigPendingMap.get(cacheKey) === request) {
        approvalConfigCache.set(cacheKey, config);
      }
      return config;
    })
    .finally(() => {
      if (approvalConfigPendingMap.get(cacheKey) === request) {
        approvalConfigPendingMap.delete(cacheKey);
      }
    });

  approvalConfigPendingMap.set(cacheKey, request);

  return request;
}
