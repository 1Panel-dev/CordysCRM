import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';

import CDR from '@/api/http/index';

import {
  addDepartmentUrl,
  addUserUrl,
  batchEditUserUrl,
  batchEnableUserUrl,
  batchResetPasswordUrl,
  checkDeleteDepartmentUrl,
  deleteDepartmentUrl,
  deleteUserCheckUrl,
  deleteUserUrl,
  getDepartmentTreeUrl,
  getRoleOptionsUrl,
  getUserDetailUrl,
  getUserListUrl,
  getUserOptionsUrl,
  importUserPreCheckUrl,
  importUserUrl,
  renameDepartmentUrl,
  resetUserPasswordUrl,
  setCommanderUrl,
  syncOrgUrl,
  updateUserUrl,
} from '@lib/shared/api/requrls/system/org';
import type { CommonList } from '@lib/shared/models/common';
import type {
  DepartmentItemParams,
  MemberItem,
  MemberParams,
  SetCommanderParams,
  UpdateDepartmentItemParams,
  UserTableQueryParams,
  ValidateInfo,
} from '@lib/shared/models/system/org';

// 组织架构-部门树查询
export function getDepartmentTree() {
  return CDR.get<CrmTreeNodeData[]>({ url: getDepartmentTreeUrl });
}

// 组织架构-添加子部门
export function addDepartment(data: DepartmentItemParams) {
  return CDR.post({ url: addDepartmentUrl, data });
}

// 组织架构-重命名部门
export function renameDepartment(data: UpdateDepartmentItemParams) {
  return CDR.post({ url: renameDepartmentUrl, data });
}

// 组织架构-设置部门负责人
export function setCommander(data: SetCommanderParams) {
  return CDR.post({ url: setCommanderUrl, data });
}

// 组织架构-删除部门
export function deleteDepartment(id: string) {
  return CDR.get({ url: `${deleteDepartmentUrl}/${id}` });
}

// 组织架构-删除部门校验
export function checkDeleteDepartment(id: string) {
  return CDR.get({ url: `${checkDeleteDepartmentUrl}/${id}` });
}

// 用户(员工)-添加员工
export function addUser(data: MemberParams) {
  return CDR.post({ url: addUserUrl, data });
}

// 用户(员工)-更新员工
export function updateUser(data: MemberParams) {
  return CDR.post({ url: updateUserUrl, data });
}

// 用户(员工)-列表查询
export function getUserList(data: UserTableQueryParams) {
  return CDR.post<CommonList<MemberItem>>({ url: getUserListUrl, data });
}

// 用户(员工)-员工详情
export function getUserDetail(userId: string) {
  return CDR.get<MemberParams>({ url: `${getUserDetailUrl}/${userId}` });
}

// 用户(员工)-批量启用|禁用
export function batchToggleStatusUser(data: UserTableQueryParams) {
  return CDR.post({ url: batchEnableUserUrl, data });
}

// 用户(员工)-批量重置密码
export function batchResetUserPassword(data: UserTableQueryParams) {
  return CDR.post({ url: batchResetPasswordUrl, data });
}

// 用户(员工)-重置密码
export function resetUserPassword(userId: string) {
  return CDR.get({ url: `${resetUserPasswordUrl}/${userId}` });
}

// 用户(员工)- 同步组织架构 TODO 类型
export function syncOrg(type: string) {
  return CDR.get({ url: `${syncOrgUrl}/${type}` });
}

// 用户(员工)-批量编辑
export function batchEditUser(data: UserTableQueryParams) {
  return CDR.post({ url: batchEditUserUrl, data });
}

// 用户(员工)-excel导入检查
export function importUserPreCheck(file: File) {
  return CDR.uploadFile<{ data: ValidateInfo }>({ url: importUserPreCheckUrl }, { fileList: [file] }, 'file');
}

// 用户(员工)-获取用户下拉
export function getUserOptions() {
  return CDR.get({ url: getUserOptionsUrl });
}

// 用户(员工)-获取角色下拉
export function getRoleOptions() {
  return CDR.get({ url: getRoleOptionsUrl });
}

// 用户(员工)-excel导入
export function importUsers(file: File) {
  return CDR.uploadFile({ url: importUserUrl }, { fileList: [file] }, 'file');
}

// 用户(员工)-删除员工
export function deleteUser(userId: string) {
  return CDR.get({ url: `${deleteUserUrl}/${userId}` });
}
// 用户(员工)-删除员工校验
export function deleteUserCheck(userId: string) {
  return CDR.get({ url: `${deleteUserCheckUrl}/${userId}` });
}
