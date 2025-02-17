import { getRoleDeptUserTree, getRoleMemberTree, getUsers } from '@/api/modules/system/role';

import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
// 添加部门、角色、成员数据API
export const getDataApiMap: Record<MemberApiTypeEnum, Record<MemberSelectTypeEnum, (params: any) => Promise<any[]>>> = {
  [MemberApiTypeEnum.SYSTEM_ROLE]: {
    [MemberSelectTypeEnum.ORG]: getRoleDeptUserTree,
    [MemberSelectTypeEnum.ROLE]: getRoleMemberTree,
    [MemberSelectTypeEnum.MEMBER]: getUsers,
  },
};

// 获取部门、角色、成员数据
export function getDataFunc(apiType: MemberApiTypeEnum, activeType: MemberSelectTypeEnum, params: Record<string, any>) {
  return getDataApiMap[apiType][activeType](params);
}
