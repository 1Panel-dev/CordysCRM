import type { MemberApiTypeEnum } from '@lib/shared/enums/moduleEnum';
import type { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';

import type { Option } from '../crm-select-user-drawer/type';

export type UserTagSelectorProps = {
  apiTypeKey?: MemberApiTypeEnum; // 要配置对应的key
  disabledNodeTypes?: DeptNodeTypeEnum[]; // 禁用的节点类型
  userErrorTagIds?: string[];
  multiple?: boolean;
  drawerTitle?: string;
  okText?: string;
  memberTypes?: Option[];
  disabled?: boolean;
  fetchOrgParams?: Record<string, any>; // 组织架构入参
  fetchRoleParams?: Record<string, any>; // 角色入参
  fetchMemberParams?: Record<string, any>; // 成员入参
  baseParams?: Record<string, any>; // 基础公共入参
};
