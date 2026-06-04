import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddCustomFormUrl,
  BatchRemoveCustomFormMemberUrl,
  GetCustomFormAdminUrl,
  GetCustomFormMemberUrl,
  GetCustomFormUrl,
  RelateCustomFormMemberUrl,
  RemoveCustomFormMemberUrl,
  SaveCustomFormAdminUrl,
  UpdateCustomFormUrl,
} from '@lib/shared/api/requrls/customForm';
import type { CommonList } from '@lib/shared/models/common';
import type {
  CustomFormAdminParams,
  CustomFormDetail,
  CustomFormMemberItem,
  CustomFormMemberTableQueryParams,
  CustomFormSaveRequest,
  RelateCustomFormMemberParams,
} from '@lib/shared/models/customForm';
import type { SelectedUsersItem } from '@lib/shared/models/system/module';

export default function useCustomFormApi(CDR: CordysAxios) {
  function addCustomForm(data: CustomFormSaveRequest) {
    return CDR.post({ url: AddCustomFormUrl, data });
  }

  function updateCustomForm(data: CustomFormSaveRequest) {
    return CDR.post({ url: UpdateCustomFormUrl, data });
  }

  function getCustomFormDetail(id: string) {
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

  function getCustomFormMember(data: CustomFormMemberTableQueryParams) {
    return CDR.post<CommonList<CustomFormMemberItem>>({ url: GetCustomFormMemberUrl, data });
  }

  function removeCustomFormMember(id: string) {
    return CDR.get({ url: `${RemoveCustomFormMemberUrl}/${id}` });
  }

  function batchRemoveCustomFormMember(data: (string | number)[]) {
    return CDR.post({ url: BatchRemoveCustomFormMemberUrl, data });
  }

  return {
    addCustomForm,
    updateCustomForm,
    getCustomFormDetail,
    saveCustomFormAdmins,
    getCustomFormAdmins,
    relateCustomFormMember,
    getCustomFormMember,
    removeCustomFormMember,
    batchRemoveCustomFormMember,
  };
}
