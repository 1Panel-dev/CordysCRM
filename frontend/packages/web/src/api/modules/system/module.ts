import CDR from '@/api/http/index';

import {
  getModuleNavConfigListUrl,
  moduleNavListSortUrl,
  toggleModuleNavStatusUrl,
} from '@lib/shared/api/requrls/system/module';
import type { ModuleNavBaseInfoItem, ModuleSortParams } from '@lib/shared/models/system/module';

// 模块首页-导航模块列表
export function getModuleNavConfigList(data: { organizationId: string }) {
  return CDR.post<ModuleNavBaseInfoItem[]>({ url: getModuleNavConfigListUrl, data });
}

// 模块首页-导航模块排序
export function moduleNavListSort(data: ModuleSortParams) {
  return CDR.post({ url: moduleNavListSortUrl, data });
}

// 模块首页-导航模块状态切换
export function toggleModuleNavStatus(id: string) {
  return CDR.get({ url: `${toggleModuleNavStatusUrl}/${id}` });
}
