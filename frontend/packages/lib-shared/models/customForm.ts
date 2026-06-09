import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';
import type { FormConfig } from '@lib/shared/models/system/module';
import type { RoleMemberRoleItem } from '@lib/shared/models/system/role';
import type { ModuleField, TableQueryParams } from '@lib/shared/models/common';
import type { SelectedUsersItem } from '@lib/shared/models/system/module';
export interface CustomFormSaveRequest {
  id?: string;
  name: string;
  enable: boolean;
  fields: FormCreateField[];
  formProp: FormConfig;
}

export interface CustomFormDetail extends CustomFormSaveRequest {
  id: string;
  creator: SelectedUsersItem;
}

export interface CustomFormAdminParams {
  customFormId: string;
  userIds: string[];
}

export interface CustomFormRoleItem {
  id: string;
  name: string;
  customFormId: string;
  internalKey: string;
}

export interface CustomFormRoleUserQueryParams extends TableQueryParams {
  customFormRoleId: string;
}

export interface RelateCustomFormMemberParams {
  customFormRoleId: string;
  deptIds?: string[];
  roleIds?: string[];
  userIds?: string[];
}

export interface CustomFormMemberItem {
  id: string;
  userId: string;
  username: string;
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
  hasCreateDataPermission: boolean;
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
  isAdmin: boolean;
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
