import type { TableQueryParams } from '../common';

// 添加部门
export interface DepartmentItemParams {
  name: string;
  parentId: string;
}

export interface UpdateDepartmentItemParams {
  name: string;
  id: string;
}

// 设置部门负责人
export interface SetCommanderParams {
  departmentId: string;
  commanderId: string;
}

export interface MemberRoleItem {
  id: string;
  name: string;
  userId: string;
}

export interface UserTableQueryParams extends TableQueryParams {
  departmentId?: string;
}

export interface BaseMemberInfo {
  phone: string;
  gender: boolean; // 性别(false-男/true-女)
  email: string;
  name: string;
  departmentId: string | null;
  position: string; // 职位
  employeeId: string; // 工号
  employeeType: string | null; // 员工类型
  supervisorId: string | null; // 直属上级
  workCity: string | null; // 工作城市
  enable: boolean; // 是否启用
  roleIds: string[]; // 角色
  userGroupIds: string[]; // 用户组
}

export interface MemberItem extends BaseMemberInfo {
  id: string;
  userId: string; // 用户ID
  userName: string;
  organizationId: string;
  roles: MemberRoleItem[];
  userGroups: MemberRoleItem[];
  createUser: string;
  createUserName: string;
  updateUser: string;
  updateUserName: string;
  createTime: number;
  updateTime: number;
  departmentName: string;
}

export interface MemberParams extends BaseMemberInfo {
  id?: string;
  userName: string;
}
