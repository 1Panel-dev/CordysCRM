import { showFailToast } from 'vant';

import createAxios from '@lib/shared/api/http';
import useClueApi from '@lib/shared/api/modules/clue';
import useCustomerApi from '@lib/shared/api/modules/customer';
import useOpportunityApi from '@lib/shared/api/modules/opportunity';
import useProductApi from '@lib/shared/api/modules/product';
import useLicenseApi from '@lib/shared/api/modules/system/authorizedManagement';
import useBusinessApi from '@lib/shared/api/modules/system/business';
import useLoginApi from '@lib/shared/api/modules/system/login';
import useMessageApi from '@lib/shared/api/modules/system/message';
import useModuleApi from '@lib/shared/api/modules/system/module';
import useOrgApi from '@lib/shared/api/modules/system/org';

import checkStatus from '../http/checkStatus';
import {AdvancedSearchOptPageUrl} from "@lib/shared/api/requrls/opportunity";

const CDR = createAxios({
  showErrorMsg: showFailToast,
  checkStatus,
});

const productApi = useProductApi(CDR);
const opportunityApi = useOpportunityApi(CDR);
const clueApi = useClueApi(CDR);
const customerApi = useCustomerApi(CDR);
const messageApi = useMessageApi(CDR);
const moduleApi = useModuleApi(CDR);
const orgApi = useOrgApi(CDR);
const businessApi = useBusinessApi(CDR);
const loginApi = useLoginApi(CDR);
const licenseApi = useLicenseApi(CDR);

export const {
  addProduct,
  getProduct,
  getProductFormConfig,
  getProductList,
  deleteProduct,
  updateProduct,
  batchDeleteProduct,
  batchUpdateProduct,
  dragSortProduct,
} = productApi;

export const {
  getOpportunityList,
  addOpportunity,
  updateOpportunity,
  getOpportunityDetail,
  getOptFormConfig,
  getOptFollowRecordList,
  deleteOptFollowRecord,
  addOptFollowRecord,
  updateOptFollowRecord,
  getOptFollowRecord,
  getOptFollowPlanList,
  addOptFollowPlan,
  updateOptFollowPlan,
  deleteOptFollowPlan,
  getOptFollowPlan,
  cancelOptFollowPlan,
  transferOpt,
  batchDeleteOpt,
  deleteOpt,
  updateOptStage,
  getOptTab,
  getOpportunityContactList,
  updateOptFollowPlanStatus,
  advancedSearchOptPage,
} = opportunityApi;

export const {
  addClue,
  updateClue,
  updateClueStatus,
  getClueList,
  batchTransferClue,
  batchToCluePool,
  batchDeleteClue,
  getClueFormConfig,
  getClue,
  deleteClue,
  ClueTransitionCustomer,
  addClueFollowRecord,
  updateClueFollowRecord,
  getClueFollowRecordList,
  deleteClueFollowRecord,
  getClueFollowRecord,
  addClueFollowPlan,
  updateClueFollowPlan,
  deleteClueFollowPlan,
  getClueFollowPlanList,
  getClueFollowPlan,
  cancelClueFollowPlan,
  getClueHeaderList,
  pickClue,
  getCluePoolList,
  batchPickClue,
  batchDeleteCluePool,
  batchAssignClue,
  assignClue,
  getPoolOptions,
  deleteCluePool,
  getCluePoolFollowRecordList,
  getPoolClue,
  getClueTab,
  getClueTransitionCustomerList,
  updateClueFollowPlanStatus,
  getAdvancedCluePoolList,
  getAdvancedSearchClueList,
} = clueApi;

export const { getDepartmentTree, getUserList, getUserDetail, getUserOptions, getRoleOptions } = orgApi;

export const {
  addCustomer,
  updateCustomer,
  getCustomerList,
  getCustomerFormConfig,
  getCustomer,
  deleteCustomer,
  batchDeleteCustomer,
  batchTransferCustomer,
  batchMoveCustomer,
  addCustomerFollowRecord,
  updateCustomerFollowRecord,
  deleteCustomerFollowRecord,
  getCustomerFollowRecordList,
  getCustomerFollowRecordFormConfig,
  getCustomerFollowRecord,
  addCustomerFollowPlan,
  updateCustomerFollowPlan,
  deleteCustomerFollowPlan,
  getCustomerFollowPlanList,
  cancelCustomerFollowPlan,
  getCustomerFollowPlanFormConfig,
  getCustomerFollowPlan,
  addCustomerContact,
  getCustomerContactList,
  updateCustomerContact,
  disableCustomerContact,
  getCustomerContactFormConfig,
  getCustomerContact,
  enableCustomerContact,
  deleteCustomerContact,
  checkOpportunity,
  getContactListUnderCustomer,
  addCustomerOpenSea,
  updateCustomerOpenSea,
  getCustomerOpenSeaList,
  switchCustomerOpenSea,
  deleteCustomerOpenSea,
  isCustomerOpenSeaNoPick,
  getOpenSeaCustomerList,
  pickOpenSeaCustomer,
  batchPickOpenSeaCustomer,
  batchDeleteOpenSeaCustomer,
  batchAssignOpenSeaCustomer,
  assignOpenSeaCustomer,
  getOpenSeaOptions,
  getOpenSeaCustomer,
  deleteOpenSeaCustomer,
  getCustomerHeaderList,
  saveCustomerRelation,
  getCustomerRelationList,
  getCustomerCollaborationList,
  batchDeleteCustomerCollaboration,
  updateCustomerCollaboration,
  addCustomerCollaboration,
  deleteCustomerCollaboration,
  getCustomerOptions,
  getCustomerOpenSeaFollowRecordList,
  updateCustomerRelationItem,
  deleteCustomerRelationItem,
  addCustomerRelationItem,
  getCustomerTab,
  getCustomerContactTab,
  updateCustomerFollowPlanStatus,
  getCustomerOpportunityPage,
  geAdvancedCustomerList,
  getAdvancedCustomerContactList,
  getAdvancedOpenSeaCustomerList,
} = customerApi;

export const {
  addAnnouncement,
  updateAnnouncement,
  getAnnouncementList,
  getAnnouncementDetail,
  deleteAnnouncement,
  getNotificationList,
  getNotificationCount,
  setNotificationRead,
  setAllNotificationRead,
  getMessageTask,
  saveMessageTask,
  batchSaveMessageTask,
  getHomeMessageList,
  closeMessageSubscribe,
  getUnReadAnnouncement,
} = messageApi;

export const {
  getModuleNavConfigList,
  getModuleUserDeptTree,
  getModuleRoleTree,
  getOpportunityRuleList,
  getCluePoolPage,
  noPickCluePool,
  getCapacityPage,
  getCustomerPoolPage,
  noPickCustomerPool,
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
  getReasonConfig,
} = moduleApi;

export const {
  updatePersonalInfo,
  getPersonalInfo,
  sendEmailCode,
  updateUserPassword,
  GetRepeatClueDetailList,
  GetRepeatClueList,
  GetRepeatCustomerList,
  GetRepeatOpportunityDetailList,
  getThirdConfigByType,
  exportCenterDownload,
  getExportCenterList,
  cancelCenterExport,
  getRepeatContactList,
  getConfigSynchronization,
} = businessApi;

export const { isLogin, signout, getKey, login, getWeComOauthCallback, getWeComCallback } = loginApi;

export const { getLicense, addLicense } = licenseApi;
