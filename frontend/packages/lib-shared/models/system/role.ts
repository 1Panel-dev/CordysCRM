import type { DeptNodeTypeEnum } from '../../enums/systemEnum';
import type { TableQueryParams } from '../common';

// 角色关联用户分页列表接口参数
export interface RoleMemberTableQueryParams extends TableQueryParams {
  roleId?: string;
}

// 角色关联用户接口参数
export interface RelateRoleMemberParams {
  roleId: string;
  deptIds?: string[];
  roleIds?: string[];
  userIds?: string[];
}

export interface PermissionItem {
  id: string;
  name: string;
  enable: boolean;
  license: boolean;
}

// 权限树节点
export interface PermissionTreeNode {
  id: string;
  name: string;
  license: boolean;
  enable: boolean;
  permissions: PermissionItem[];
  children: PermissionTreeNode[];
}

export interface RolePermissionItem {
  id: string;
  enable: boolean;
}

export interface Role {
  name: string;
  dataScope?: string;
  deptIds?: string[];
  description?: string;
}

export interface RoleCreateParams extends Role {
  permissions: RolePermissionItem[];
}

export interface RoleUpdateParams extends Role {
  id: string;
  permissions?: RolePermissionItem[];
}

export interface RoleDetail extends Role {
  id: string;
  deptIds: string[];
  permissions: PermissionTreeNode[];
}

// 部门用户树成员
export interface DeptTreeMember {
  id: string;
  name: string;
  parentId: string;
  organizationId: string;
}

export type DeptNodeType = DeptNodeTypeEnum;
// 部门用户树节点
export interface DeptUserTreeNode {
  id: string;
  name: string;
  parentId: string;
  organizationId: string;
  children?: DeptTreeMember[];
  nodeType?: DeptNodeType;
  enabled?: boolean; // 是否启用
}

// 部门树节点
export interface DeptTreeNode {
  id: string;
  name: string;
  parentId: string;
  organizationId: string;
}

// 角色列表项
export interface RoleItem {
  id: string;
  createUser?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  name: string;
  internal: boolean; // 是否内置
  dataScope: string;
  description?: string;
  organizationId?: string;
  createUserName?: string;
  updateUserName?: string;
  [key: string]: any; // 前端扩展字段
}

export interface RoleMemberRoleItem {
  id: string;
  name: string;
  userId: string;
}
export interface RoleMemberItem {
  id: string;
  userId: string;
  userName: string;
  enable: boolean;
  departmentId: string;
  departmentName: string;
  position: string;
  createTime: number;
  roles: RoleMemberRoleItem[];
}
