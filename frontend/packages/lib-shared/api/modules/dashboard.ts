import type { CommonList, TableQueryParams } from '../../models/common';
import type {
  DashboardAddModuleParams,
  DashboardAddParams,
  DashboardDetail,
  DashboardDragParams,
  DashboardModuleDragParams,
  DashboardModuleRenameParams,
  DashboardRenameParams,
  DashboardTableItem,
  DashboardTableQueryParams,
  DashboardUpdateParams,
} from '../../models/dashboard';
import {
  dashboardAddUrl,
  dashboardCollectPageUrl,
  dashboardCollectUrl,
  dashboardDeleteUrl,
  dashboardDetailUrl,
  dashboardDragUrl,
  dashboardModuleAddUrl,
  dashboardModuleCountUrl,
  dashboardModuleDeleteUrl,
  dashboardModuleDragUrl,
  dashboardModuleRenameUrl,
  dashboardModuleTreeUrl,
  dashboardPageUrl,
  dashboardRenameUrl,
  dashboardUnCollectUrl,
  dashboardUpdateUrl,
} from '../requrls/dashboard';
import type { CordysAxios } from '@lib/shared/api/http/Axios';

export default function useDashboardApi(CDR: CordysAxios) {
  // 重命名仪表板模块
  function dashboardModuleRename(data: DashboardModuleRenameParams) {
    return CDR.post({ url: dashboardModuleRenameUrl, data });
  }

  // 删除仪表板模块
  function dashboardModuleDelete(ids: string[]) {
    return CDR.post({ url: dashboardModuleDeleteUrl, data: ids });
  }

  // 添加仪表板模块
  function dashboardModuleAdd(data: DashboardAddModuleParams) {
    return CDR.post({ url: dashboardModuleAddUrl, data });
  }

  // 更新仪表板
  function dashboardUpdate(data: DashboardUpdateParams) {
    return CDR.post({ url: dashboardUpdateUrl, data });
  }

  // 重命名仪表板
  function dashboardRename(data: DashboardRenameParams) {
    return CDR.post({ url: dashboardRenameUrl, data });
  }

  // 添加仪表板
  function dashboardAdd(data: DashboardAddParams) {
    return CDR.post({ url: dashboardAddUrl, data });
  }

  // 获取仪表板详情
  function dashboardDetail(id: string) {
    return CDR.get<DashboardDetail>({ url: `${dashboardDetailUrl}/${id}` });
  }

  // 删除仪表板
  function dashboardDelete(id: string) {
    return CDR.get({ url: `${dashboardDeleteUrl}/${id}` });
  }

  // 获取仪表板模块树
  function dashboardModuleTree() {
    return CDR.get({ url: dashboardModuleTreeUrl });
  }

  // 获取仪表板模块数量
  function dashboardModuleCount() {
    return CDR.get({ url: dashboardModuleCountUrl });
  }

  // 仪表板拖拽
  function dashboardDrag(data: DashboardDragParams) {
    return CDR.post({ url: dashboardDragUrl, data });
  }

  // 仪表板模块拖拽
  function dashboardModuleDrag(data: DashboardModuleDragParams) {
    return CDR.post({ url: dashboardModuleDragUrl, data });
  }

  // 获取仪表板列表
  function dashboardPage(data: DashboardTableQueryParams) {
    return CDR.post<CommonList<DashboardTableItem>>({ url: dashboardPageUrl, data });
  }

  // 获取仪表板收藏列表
  function dashboardCollectPage(data: TableQueryParams) {
    return CDR.post<CommonList<DashboardTableItem>>({ url: dashboardCollectPageUrl, data });
  }

  // 收藏仪表板
  function dashboardCollect(id: string) {
    return CDR.get({ url: `${dashboardCollectUrl}/${id}` });
  }

  // 取消收藏仪表板
  function dashboardUnCollect(id: string) {
    return CDR.get({ url: `${dashboardUnCollectUrl}/${id}` });
  }

  return {
    dashboardModuleRename,
    dashboardModuleDelete,
    dashboardModuleAdd,
    dashboardUpdate,
    dashboardRename,
    dashboardAdd,
    dashboardDetail,
    dashboardDelete,
    dashboardModuleTree,
    dashboardPage,
    dashboardCollectPage,
    dashboardCollect,
    dashboardUnCollect,
    dashboardModuleCount,
    dashboardModuleDrag,
    dashboardDrag,
  };
}
