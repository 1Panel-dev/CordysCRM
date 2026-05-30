import type { CordysAxios } from '@lib/shared/api/http/Axios';
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

export default function useProductApi(CDR: CordysAxios) {
  // 角色关联用户
  function relateRoleMember(data: RelateRoleMemberParams) {
    return CDR.post({ url: RelateRoleUrl, data });
  }

  // 获取角色关联用户列表
  function getRoleMember(data: RoleMemberTableQueryParams) {
    return CDR.post<CommonList<RoleMemberItem>>({ url: GetRoleMemberUrl, data });
  }

  // 批量移除角色关联用户
  function batchRemoveRoleMember(data: (string | number)[]) {
    return CDR.post({ url: BatchRemoveRoleMemberUrl, data });
  }

  // 更新角色
  function updateRole(data: RoleUpdateParams) {
    return CDR.post({ url: UpdateRoleUrl, data });
  }

  // 新建角色
  function createRole(data: RoleCreateParams) {
    return CDR.post({ url: CreateRoleUrl, data });
  }

  // 获取角色关联用户树
  function getRoleMemberTree(params: { roleId: string }) {
    return CDR.get({ url: `${GetRoleMemberTreeUrl}/${params.roleId}` });
  }

  // 获取部门用户树
  function getRoleDeptUserTree(params: { roleId: string }) {
    return CDR.get<DeptUserTreeNode[]>({ url: `${GetRoleDeptTreeUrl}/${params.roleId}` });
  }

  // 获取部门树
  function getRoleDeptTree() {
    return CDR.get<DeptTreeNode[]>({ url: GetDeptTreeUrl });
  }

  // 移除角色关联用户
  function removeRoleMember(id: string) {
    return CDR.get({ url: `${RemoveRoleMemberUrl}/${id}` });
  }

  // 获取全量权限
  function getPermissions() {
    return CDR.get<PermissionTreeNode[]>({ url: GetPermissionsUrl });
  }

  // 获取角色列表
  function getRoles() {
    return CDR.get<RoleItem[]>({ url: GetRolesUrl });
  }

  // 获取角色详情
  function getRoleDetail(id: string) {
    return CDR.get<RoleDetail>({ url: `${GetRoleDetailUrl}/${id}` });
  }

  // 删除角色
  function deleteRole(id: string) {
    return CDR.get({ url: `${DeleteRoleUrl}/${id}` });
  }

  // 获取用户列表
  function getUsers(data: { roleId: string }) {
    return CDR.get<RoleItem[]>({ url: `${GetUserOptionUrl}/${data.roleId}` });
  }

  return {
    relateRoleMember,
    getRoleMember,
    batchRemoveRoleMember,
    updateRole,
    createRole,
    getRoleMemberTree,
    getRoleDeptUserTree,
    getRoleDeptTree,
    removeRoleMember,
    getPermissions,
    getRoles,
    getRoleDetail,
    deleteRole,
    getUsers,
  };
}
