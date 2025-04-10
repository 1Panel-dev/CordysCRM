import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddCluePoolUrl,
  AddCustomerPoolUrl,
  addOpportunityRuleUrl,
  DeleteCluePoolUrl,
  DeleteCustomerPoolUrl,
  deleteOpportunityUrl,
  DownloadPictureUrl,
  GetClueCapacityPageUrl,
  GetCluePoolPageUrl,
  GetCustomerCapacityPageUrl,
  GetCustomerPoolPageUrl,
  GetFieldClueListUrl,
  GetFieldContactListUrl,
  GetFieldCustomerListUrl,
  GetFieldDeptTreeUrl,
  GetFieldDeptUerTreeUrl,
  GetFieldOpportunityListUrl,
  GetFieldProductListUrl,
  GetFormDesignConfigUrl,
  getModuleNavConfigListUrl,
  getOpportunityListUrl,
  moduleNavListSortUrl,
  ModuleRoleTreeUrl,
  ModuleUserDeptTreeUrl,
  NoPickCluePoolUrl,
  NoPickCustomerPoolUrl,
  PreviewPictureUrl,
  SaveClueCapacityUrl,
  SaveCustomerCapacityUrl,
  SaveFormDesignConfigUrl,
  SwitchCluePoolStatusUrl,
  SwitchCustomerPoolStatusUrl,
  switchOpportunityStatusUrl,
  toggleModuleNavStatusUrl,
  UpdateCluePoolUrl,
  UpdateCustomerPoolUrl,
  updateOpportunityRuleUrl,
  UploadTempFileUrl,
} from '@lib/shared/api/requrls/system/module';
import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
import type { ClueListItem } from '@lib/shared/models/clue';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type { CustomerContractListItem, CustomerListItem } from '@lib/shared/models/customer';
import type { ProductListItem } from '@lib/shared/models/product';
import type {
  CapacityItem,
  CapacityParams,
  CluePoolItem,
  CluePoolParams,
  FormDesignConfigDetailParams,
  FormDesignDataSourceTableQueryParams,
  ModuleNavBaseInfoItem,
  ModuleSortParams,
  OpportunityItem,
  OpportunityParams,
  SaveFormDesignConfigParams,
} from '@lib/shared/models/system/module';
import type { DeptUserTreeNode } from '@lib/shared/models/system/role';
import type { Result } from '@lib/shared/types/axios';

export default function useProductApi(CDR: CordysAxios) {
  // 模块首页-导航模块列表
  function getModuleNavConfigList(data: { organizationId: string }) {
    return CDR.post<ModuleNavBaseInfoItem[]>({ url: getModuleNavConfigListUrl, data });
  }

  // 模块首页-导航模块排序
  function moduleNavListSort(data: ModuleSortParams) {
    return CDR.post({ url: moduleNavListSortUrl, data });
  }

  // 模块首页-导航模块状态切换
  function toggleModuleNavStatus(id: string) {
    return CDR.get({ url: `${toggleModuleNavStatusUrl}/${id}` });
  }

  // 获取部门用户树
  function getModuleUserDeptTree() {
    return CDR.get<DeptUserTreeNode[]>({ url: ModuleUserDeptTreeUrl });
  }
  // 获取角色树
  function getModuleRoleTree() {
    return CDR.get<DeptUserTreeNode[]>({ url: ModuleRoleTreeUrl });
  }

  // 模块-商机-商机规则列表
  function getOpportunityRuleList(data: TableQueryParams) {
    return CDR.post<CommonList<OpportunityItem>>({ url: getOpportunityListUrl, data });
  }

  // 模块-商机-添加商机规则
  function addOpportunityRule(data: OpportunityParams) {
    return CDR.post({ url: addOpportunityRuleUrl, data });
  }

  // 模块-商机-更新商机规则
  function updateOpportunityRule(data: OpportunityParams) {
    return CDR.post({ url: updateOpportunityRuleUrl, data });
  }

  // 模块-商机-更新商机规则状态
  function switchOpportunityStatus(ruleId: string) {
    return CDR.get({ url: `${switchOpportunityStatusUrl}/${ruleId}` });
  }

  // 模块-商机-删除商机规则
  function deleteOpportunity(ruleId: string) {
    return CDR.get({ url: `${deleteOpportunityUrl}/${ruleId}` });
  }

  // 线索池相关API
  function getCluePoolPage(data: TableQueryParams) {
    return CDR.post<CommonList<CluePoolItem>>({ url: GetCluePoolPageUrl, data });
  }

  function addCluePool(data: CluePoolParams) {
    return CDR.post({ url: AddCluePoolUrl, data });
  }

  function updateCluePool(data: CluePoolParams) {
    return CDR.post({ url: UpdateCluePoolUrl, data });
  }

  function switchCluePoolStatus(id: string) {
    return CDR.get({ url: `${SwitchCluePoolStatusUrl}/${id}` });
  }

  function deleteModuleCluePool(id: string) {
    return CDR.get({ url: `${DeleteCluePoolUrl}/${id}` });
  }

  function noPickCluePool(id: string) {
    return CDR.get({ url: `${NoPickCluePoolUrl}/${id}` });
  }

  // 库容相关API
  function getCapacityPage(type: ModuleConfigEnum) {
    return CDR.get<CapacityItem[]>({
      url: type === ModuleConfigEnum.CLUE_MANAGEMENT ? GetClueCapacityPageUrl : GetCustomerCapacityPageUrl,
    });
  }

  function saveCapacity(data: CapacityParams[], type: ModuleConfigEnum) {
    return CDR.post({
      url: type === ModuleConfigEnum.CLUE_MANAGEMENT ? SaveClueCapacityUrl : SaveCustomerCapacityUrl,
      data,
    });
  }

  // 公海相关API
  function getCustomerPoolPage(data: TableQueryParams) {
    return CDR.post<CommonList<CluePoolItem>>({ url: GetCustomerPoolPageUrl, data });
  }

  function addCustomerPool(data: CluePoolParams) {
    return CDR.post({ url: AddCustomerPoolUrl, data });
  }

  function updateCustomerPool(data: CluePoolParams) {
    return CDR.post({ url: UpdateCustomerPoolUrl, data });
  }

  function switchCustomerPoolStatus(id: string) {
    return CDR.get({ url: `${SwitchCustomerPoolStatusUrl}/${id}` });
  }

  function deleteCustomerPool(id: string) {
    return CDR.get({ url: `${DeleteCustomerPoolUrl}/${id}` });
  }

  function noPickCustomerPool(id: string) {
    return CDR.get({ url: `${NoPickCustomerPoolUrl}/${id}` });
  }

  // 表单设计
  function saveFormDesignConfig(data: SaveFormDesignConfigParams) {
    return CDR.post({ url: SaveFormDesignConfigUrl, data });
  }

  function getFormDesignConfig(id: string) {
    return CDR.get<FormDesignConfigDetailParams>({ url: `${GetFormDesignConfigUrl}/${id}` });
  }

  function getFieldDeptUerTree() {
    return CDR.get<DeptUserTreeNode[]>({ url: GetFieldDeptUerTreeUrl });
  }

  function getFieldDeptTree() {
    return CDR.get<DeptUserTreeNode[]>({ url: GetFieldDeptTreeUrl });
  }

  function getFieldClueList(data: FormDesignDataSourceTableQueryParams) {
    return CDR.post<CommonList<ClueListItem>>({ url: GetFieldClueListUrl, data });
  }

  function getFieldContactList(data: FormDesignDataSourceTableQueryParams) {
    return CDR.post<CommonList<CustomerContractListItem>>({ url: GetFieldContactListUrl, data });
  }

  function getFieldCustomerList(data: FormDesignDataSourceTableQueryParams) {
    return CDR.post<CommonList<CustomerListItem>>({ url: GetFieldCustomerListUrl, data });
  }

  function getFieldOpportunityList(data: FormDesignDataSourceTableQueryParams) {
    return CDR.post<CommonList<OpportunityItem>>({ url: GetFieldOpportunityListUrl, data });
  }

  function getFieldProductList(data: FormDesignDataSourceTableQueryParams) {
    return CDR.post<CommonList<ProductListItem>>({ url: GetFieldProductListUrl, data });
  }

  function uploadTempFile(file: File | null) {
    return CDR.uploadFile<Result<string[]>>({ url: UploadTempFileUrl }, { fileList: [file] }, 'files', true);
  }

  function previewPicture(id: string) {
    return CDR.get({ url: `${PreviewPictureUrl}/${id}` });
  }

  function downloadPicture(id: string) {
    return CDR.get({ url: `${DownloadPictureUrl}/${id}` });
  }

  return {
    getModuleNavConfigList,
    moduleNavListSort,
    toggleModuleNavStatus,
    getModuleUserDeptTree,
    getModuleRoleTree,
    getOpportunityRuleList,
    addOpportunityRule,
    updateOpportunityRule,
    switchOpportunityStatus,
    deleteOpportunity,
    getCluePoolPage,
    addCluePool,
    updateCluePool,
    switchCluePoolStatus,
    deleteModuleCluePool,
    noPickCluePool,
    getCapacityPage,
    saveCapacity,
    getCustomerPoolPage,
    addCustomerPool,
    updateCustomerPool,
    switchCustomerPoolStatus,
    deleteCustomerPool,
    noPickCustomerPool,
    saveFormDesignConfig,
    getFormDesignConfig,
    getFieldDeptUerTree,
    getFieldDeptTree,
    getFieldClueList,
    getFieldContactList,
    getFieldCustomerList,
    getFieldOpportunityList,
    getFieldProductList,
    uploadTempFile,
    previewPicture,
    downloadPicture,
  };
}
