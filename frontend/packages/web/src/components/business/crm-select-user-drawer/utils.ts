import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';

import { getModuleRoleTree, getModuleUserDeptTree } from '@/api/modules/system/module';
import { getRoleDeptUserTree, getRoleMemberTree, getUsers } from '@/api/modules/system/role';
// 添加部门、角色、成员数据API
export const getDataApiMap: Record<
  MemberApiTypeEnum,
  Partial<Record<MemberSelectTypeEnum, (params: any) => Promise<any[]>>>
> = {
  [MemberApiTypeEnum.SYSTEM_ROLE]: {
    [MemberSelectTypeEnum.ORG]: getRoleDeptUserTree,
    [MemberSelectTypeEnum.ROLE]: getRoleMemberTree,
    [MemberSelectTypeEnum.MEMBER]: getUsers,
  },
  [MemberApiTypeEnum.MODULE_ROLE]: {
    [MemberSelectTypeEnum.ORG]: getModuleUserDeptTree,
    [MemberSelectTypeEnum.ROLE]: getModuleRoleTree,
  },
};

// 获取部门、角色、成员数据
export function getDataFunc(apiType: MemberApiTypeEnum, activeType: MemberSelectTypeEnum, params: Record<string, any>) {
  const func = getDataApiMap[apiType]?.[activeType];
  if (!func) {
    return Promise.reject();
  }
  return func(params);
}
