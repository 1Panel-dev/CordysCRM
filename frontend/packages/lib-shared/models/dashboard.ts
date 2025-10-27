import type { TableQueryParams } from './common';
import type { SelectedUsersItem } from './system/module';
import type { CrmTreeNodeData } from '@cordys/web/src/components/pure/crm-tree/type';

export interface DashboardModuleRenameParams {
  id: string;
  name: string;
}

export interface DashboardAddModuleParams {
  parentId: string;
  name: string;
}

export interface DashboardAddParams {
  dashboardModuleId: string;
  resourceUrl: string;
  scopeIds: string[];
  name: string;
  description: string;
}

export interface DashboardUpdateParams extends DashboardAddParams {
  id: string;
}

export interface DashboardRenameParams {
  dashboardModuleId: string;
  id: string;
  name: string;
}

export interface DashboardTableItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  name: string;
  resourceId: string;
  dashboardModuleId: string;
  organizationId: string;
  pos: number;
  scopeId: string;
  description: string;
}

export interface DashboardTableQueryParams extends TableQueryParams {
  dashboardModuleIds: string[];
}

export type DashboardModuleTreeItem = CrmTreeNodeData<{ type: 'MODULE' | 'DASHBOARD'; myCollect: boolean }>;

export interface DashboardDetail {
  dashboardModuleId: string;
  members: SelectedUsersItem[];
  id: string;
  description: string;
  dashboardModuleName: string;
  name: string;
  scopeId: string;
  resourceUrl: string;
}

export interface DashboardDragParams {
  moveId: string;
  targetId: string;
  dashboardModuleId: string;
  moveMode: string;
}

export interface DashboardModuleDragParams {
  dragNodeId: string;
  dropNodeId: string;
  dropPosition: number;
}
