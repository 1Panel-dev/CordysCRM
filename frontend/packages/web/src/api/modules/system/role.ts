import CDR from '@/api/http/index';

import {
  BatchRemoveRoleMemberUrl,
  CreateRoleUrl,
  DeleteRoleUrl,
  GetDeptTreeUrl,
  GetPermissionsUrl,
  GetRoleDeptTreeUrl,
  GetRoleDetailUrl,
  GetRoleMemberTreeUrl,
  GetRoleMemberUrl,
  GetRolesUrl,
  GetUserOptionUrl,
  RelateRoleUrl,
  RemoveRoleMemberUrl,
  UpdateRoleUrl,
} from '@lib/shared/api/requrls/system/role';
import type { CommonList } from '@lib/shared/models/common';
import type {
  DeptTreeNode,
  DeptUserTreeNode,
  PermissionTreeNode,
  RelateRoleMemberParams,
  RoleCreateParams,
  RoleDetail,
  RoleItem,
  RoleMemberItem,
  RoleMemberTableQueryParams,
  RoleUpdateParams,
} from '@lib/shared/models/system/role';

// 角色关联用户
export function relateRoleMember(data: RelateRoleMemberParams) {
  return CDR.post({ url: RelateRoleUrl, data });
}

// 获取角色关联用户列表
export function getRoleMember(data: RoleMemberTableQueryParams) {
  return CDR.post<CommonList<RoleMemberItem>>({ url: GetRoleMemberUrl, data });
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
export function getRoleMemberTree(params: { roleId: string }) {
  return CDR.get({ url: `${GetRoleMemberTreeUrl}/${params.roleId}` });
}

// 获取部门用户树
export function getRoleDeptUserTree(params: { roleId: string }) {
  return CDR.get<DeptUserTreeNode[]>({ url: `${GetRoleDeptTreeUrl}/${params.roleId}` });
}

// 获取部门树
export function getRoleDeptTree() {
  return CDR.get<DeptTreeNode[]>({ url: GetDeptTreeUrl });
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
export function getRoles() {
  return CDR.get<RoleItem[]>({ url: GetRolesUrl });
}

// 获取角色详情
export function getRoleDetail(id: string) {
  return CDR.get<RoleDetail>({ url: `${GetRoleDetailUrl}/${id}` });
}

// 删除角色
export function deleteRole(id: string) {
  return CDR.get({ url: `${DeleteRoleUrl}/${id}` });
}

// 获取用户列表
export function getUsers(data: { roleId: string }) {
  return CDR.get<RoleItem[]>({ url: `${GetUserOptionUrl}/${data.roleId}` });
}
