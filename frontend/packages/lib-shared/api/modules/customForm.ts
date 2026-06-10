import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddCustomFormUrl,
  GetCustomFormAdminUrl,
  GetCustomFormRoleUserDeptTreeUrl,
  GetCustomFormRoleUserRoleTreeUrl,
  GetCustomFormRoleListUrl,
  GetCustomFormRoleUsersUrl,
  GetCustomFormUrl,
  RelateCustomFormMemberUrl,
  RemoveCustomFormMemberUrl,
  SaveCustomFormAdminUrl,
  UpdateCustomFormUrl,
  GetCustomFormDataDetailUrl,
  GetCustomFormDataPageUrl,
  AddCustomFormDataUrl,
  UpdateCustomFormDataUrl,
  DeleteCustomFormDataUrl,
  BatchDeleteCustomFormDataUrl,
  BatchUpdateCustomFormDataUrl,
  GetCustomFormListUrl,
  GetCustomFormOptionsUrl,
  DeleteCustomFormUrl,
  EnableCustomFormUrl,
  DisableCustomFormUrl,
} from '@lib/shared/api/requrls/customForm';
import type { CommonList } from '@lib/shared/models/common';
import type {
  AddCustomFormDataParams,
  BatchUpdateCustomFormDataParams,
  CustomFormAdminParams,
  CustomFormDataDetail,
  CustomFormDetail,
  CustomFormItem,
  CustomFormMemberItem,
  CustomFormRoleItem,
  CustomFormRoleUserQueryParams,
  CustomFormPageItem,
  CustomFormSaveRequest,
  GetCustomFormDataPageParams,
  RelateCustomFormMemberParams,
  UpdateCustomFormDataParams,
} from '@lib/shared/models/customForm';
import type { SelectedUsersItem } from '@lib/shared/models/system/module';
import type { DeptUserTreeNode } from '@lib/shared/models/system/role';

export default function useCustomFormApi(CDR: CordysAxios) {
  function addCustomForm(data: CustomFormSaveRequest) {
    return CDR.post({ url: AddCustomFormUrl, data });
  }

  function updateCustomForm(data: CustomFormSaveRequest) {
    return CDR.post({ url: UpdateCustomFormUrl, data });
  }

  function getCustomFormDetail(id?: string) {
    return CDR.get<CustomFormDetail>({ url: `${GetCustomFormUrl}/${id}` });
  }

  function getCustomFormAdmins(customFormId: string) {
    return CDR.get<SelectedUsersItem[]>({ url: `${GetCustomFormAdminUrl}/${customFormId}` });
  }

  function saveCustomFormAdmins(data: CustomFormAdminParams) {
    return CDR.post({ url: SaveCustomFormAdminUrl, data });
  }

  // 表单成员
  function relateCustomFormMember(data: RelateCustomFormMemberParams) {
    return CDR.post({ url: RelateCustomFormMemberUrl, data });
  }

  function getCustomFormRoles(customFormId: string) {
    return CDR.get<CustomFormRoleItem[]>({ url: `${GetCustomFormRoleListUrl}/${customFormId}` });
  }

  function getCustomFormRoleUsers(data: CustomFormRoleUserQueryParams) {
    return CDR.post<CommonList<CustomFormMemberItem>>({ url: GetCustomFormRoleUsersUrl, data });
  }

  function getCustomFormRoleUserDeptTree() {
    return CDR.get<DeptUserTreeNode[]>({ url: GetCustomFormRoleUserDeptTreeUrl });
  }

  function getCustomFormRoleUserRoleTree() {
    return CDR.get<DeptUserTreeNode[]>({ url: GetCustomFormRoleUserRoleTreeUrl });
  }

  function removeCustomFormMember(data: RelateCustomFormMemberParams) {
    return CDR.post({ url: RemoveCustomFormMemberUrl, data });
  }

  function deleteCustomForm(id: string) {
    return CDR.get({ url: `${DeleteCustomFormUrl}/${id}` });
  }

  function enableCustomForm(id: string) {
    return CDR.get({ url: `${EnableCustomFormUrl}/${id}` });
  }

  function disableCustomForm(id: string) {
    return CDR.get({ url: `${DisableCustomFormUrl}/${id}` });
  }

  function getCustomFormList() {
    return CDR.get<CustomFormItem[]>({ url: GetCustomFormListUrl });
  }

  function getCustomFormDataDetail(id: string) {
    return CDR.get<CustomFormDataDetail>({ url: `${GetCustomFormDataDetailUrl}/${id}` });
  }

  function getCustomFormDataPage(data: GetCustomFormDataPageParams) {
    return CDR.post<CommonList<CustomFormPageItem>>({ url: GetCustomFormDataPageUrl, data });
  }

  function addCustomFormData(data: AddCustomFormDataParams) {
    return CDR.post({ url: AddCustomFormDataUrl, data });
  }

  function updateCustomFormData(data: UpdateCustomFormDataParams) {
    return CDR.post({ url: UpdateCustomFormDataUrl, data });
  }

  function deleteCustomFormData(id: string) {
    return CDR.get({ url: `${DeleteCustomFormDataUrl}/${id}` });
  }

  function batchDeleteCustomFormData(ids: string[]) {
    return CDR.post({ url: BatchDeleteCustomFormDataUrl, data: ids });
  }

  function batchUpdateCustomFormData(data: BatchUpdateCustomFormDataParams) {
    return CDR.post({ url: BatchUpdateCustomFormDataUrl, data });
  }

  function getCustomFormOptions() {
    return CDR.get<CustomFormItem[]>({ url:GetCustomFormOptionsUrl });
  }

  return {
    addCustomForm,
    updateCustomForm,
    getCustomFormDetail,
    saveCustomFormAdmins,
    getCustomFormAdmins,
    relateCustomFormMember,
    getCustomFormRoles,
    getCustomFormRoleUsers,
    getCustomFormRoleUserDeptTree,
    getCustomFormRoleUserRoleTree,
    removeCustomFormMember,
    getCustomFormList,
    getCustomFormDataDetail,
    getCustomFormDataPage,
    addCustomFormData,
    updateCustomFormData,
    deleteCustomFormData,
    batchDeleteCustomFormData,
    batchUpdateCustomFormData,
    getCustomFormOptions,
    deleteCustomForm,
    enableCustomForm,
    disableCustomForm,
  };
}
