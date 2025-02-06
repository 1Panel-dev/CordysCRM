import CDR from '@/api/http/index';

import {
  BatchRemoveRoleMemberUrl,
  CreateRoleUrl,
  DeleteRoleUrl,
  GetPermissionsUrl,
  GetRoleDeptTreeUrl,
  GetRoleDetailUrl,
  GetRoleMemberTreeUrl,
  GetRoleMemberUrl,
  GetRolesUrl,
  RelateRoleUrl,
  RemoveRoleMemberUrl,
  UpdateRoleUrl,
} from '@lib/shared/api/requrls/system/role';
import type {
  DeptTreeNode,
  PermissionTreeNode,
  RelateRoleMemberParams,
  RoleCreateParams,
  RoleDetail,
  RoleItem,
  RoleMemberTableQueryParams,
  RoleUpdateParams,
} from '@lib/shared/models/system/role';

// 角色关联用户
export function relateRoleMember(data: RelateRoleMemberParams) {
  return CDR.post({ url: RelateRoleUrl, data });
}

// 获取角色关联用户列表
export function getRoleMember(data: RoleMemberTableQueryParams) {
  return CDR.post({ url: GetRoleMemberUrl, data });
}

// 批量移除角色关联用户
export function batchRemoveRoleMember(data: string[]) {
  return CDR.post({ url: BatchRemoveRoleMemberUrl, data });
}

// 更新角色
export function updateRole(data: RoleUpdateParams) {
  return CDR.post({ url: UpdateRoleUrl, data });
}

// 新建角色
export function createRole(data: RoleCreateParams) {
  return CDR.post({ url: CreateRoleUrl, data });
}

// 获取角色关联用户树
export function getRoleMemberTree(roleId: string) {
  return CDR.get({ url: `${GetRoleMemberTreeUrl}/${roleId}` });
}

// 获取部门用户树
export function getRoleDeptTree(roleId: string) {
  return CDR.get<DeptTreeNode[]>({ url: `${GetRoleDeptTreeUrl}/${roleId}` });
}

// 移除角色关联用户
export function removeRoleMember(id: string) {
  return CDR.get({ url: `${RemoveRoleMemberUrl}/${id}` });
}

// 获取全量权限
export function getPermissions() {
  return CDR.get<PermissionTreeNode[]>({ url: GetPermissionsUrl });
}

// 获取角色列表
export function getRoles(orgId: string) {
  return CDR.get<RoleItem[]>({ url: `${GetRolesUrl}/${orgId}` });
}

// 获取角色详情
export function getRoleDetail(id: string) {
  return CDR.get<RoleDetail>({ url: `${GetRoleDetailUrl}/${id}` });
}

// 删除角色
export function deleteRole(id: string) {
  return CDR.get({ url: `${DeleteRoleUrl}/${id}` });
}
