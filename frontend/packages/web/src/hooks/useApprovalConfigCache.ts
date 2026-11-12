import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { ApprovalProcessDetail } from '@lib/shared/models/system/process';

import { getApprovalConfigDetail } from '@/api/modules';

type ApprovalConfigKey = FormDesignKeyEnum | string;

// 已经请求成功的审批流配置缓存：同一个业务类型和池后续直接复用，减少表格/详情重复请求。
const approvalConfigCache = new Map<ApprovalConfigKey, ApprovalProcessDetail | null>();
// 正在请求中的审批流配置缓存：同一时间多个组件请求同一个业务类型和池时，复用同一个 Promise，避免被 axios 判定为重复请求并取消。
const approvalConfigPendingMap = new Map<ApprovalConfigKey, Promise<ApprovalProcessDetail | null>>();

export function clearApprovalConfigCache(formKey?: ApprovalConfigKey) {
  // 审批流配置新增、更新、删除、启停后，需要清掉对应业务类型的旧缓存。
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

  // 已加载过的配置直接返回；审批流配置低频变化，变更时由 clearApprovalConfigCache 主动失效。
  if (approvalConfigCache.has(cacheKey)) {
    return Promise.resolve(approvalConfigCache.get(cacheKey) ?? null);
  }

  // 请求还没回来时，后续相同业务类型的调用等同一个请求，避免重复发起相同接口。
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
