import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';
import type { FormConfig } from '@lib/shared/models/system/module';
import type { RoleMemberRoleItem } from '@lib/shared/models/system/role';
import type { ModuleField, TableQueryParams } from '@lib/shared/models/common';

export interface CustomFormSaveRequest {
  id?: string;
  name: string;
  enable: boolean;
  fields: FormCreateField[];
  formProp: FormConfig;
}

export interface CustomFormDetail extends CustomFormSaveRequest {
  id: string;
}

export interface CustomFormAdminParams {
  customFormId: string;
  userIds: string[];
}

export enum CustomFormDataPermissionTypeEnum {
  MANAGE_ALL = 'MANAGE_ALL',
  VIEW_ALL = 'VIEW_ALL',
  ADD_MANAGE_OWN = 'ADD_MANAGE_OWN',
}

export interface CustomFormMemberTableQueryParams extends TableQueryParams {
  sourceId: string;
  permissionType: CustomFormDataPermissionTypeEnum;
}

export interface RelateCustomFormMemberParams {
  sourceId: string;
  permissionType: CustomFormDataPermissionTypeEnum;
  deptIds?: string[];
  roleIds?: string[];
  userIds?: string[];
}

export interface CustomFormMemberItem {
  id: string;
  userId: string;
  userName: string;
  departmentId: string;
  departmentName: string;
  position: string;
  createTime: number;
  roles: RoleMemberRoleItem[];
}

export interface CustomFormItem {
  id: string;
  name: string;
  enable: boolean;
  isAdmin: boolean;
}

export interface AddCustomFormDataParams {
  customFormId: string;
  name: string;
  owner: string;
  moduleFields: ModuleField[];
}

export interface UpdateCustomFormDataParams extends AddCustomFormDataParams {
  id: string;
}

export interface GetCustomFormDataPageParams extends TableQueryParams {
  customFormId: string;
}

export interface CustomFormPageItem {
  id: string;
  customFormId: string;
  name: string;
  owner: string;
  ownerName: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[];
}

export interface BatchUpdateCustomFormDataParams {
  ids: string[];
  customFormId: string;
  name: string;
  owner: string;
  moduleFields: ModuleField[];
}

export interface CustomFormDataDetail {
  id: string;
  customFormId: string;
  name: string;
  owner: string;
  ownerName: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  createUserName: string;
  updateUserName: string;
  moduleFields: ModuleField[];
  optionMap: Record<string, any>;
}
