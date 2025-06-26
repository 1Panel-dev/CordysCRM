import createAxios from '@lib/shared/api/http';
import useClueApi from '@lib/shared/api/modules/clue';
import useCustomerApi from '@lib/shared/api/modules/customer';
import useOpportunityApi from '@lib/shared/api/modules/opportunity';
import useProductApi from '@lib/shared/api/modules/product';
import useSysApi from '@lib/shared/api/modules/sys';
import useLicenseApi from '@lib/shared/api/modules/system/authorizedManagement';
import useBusinessApi from '@lib/shared/api/modules/system/business';
import useLoginApi from '@lib/shared/api/modules/system/login';
import useMessageApi from '@lib/shared/api/modules/system/message';
import useModuleApi from '@lib/shared/api/modules/system/module';
import useOrgApi from '@lib/shared/api/modules/system/org';
import useRoleApi from '@lib/shared/api/modules/system/role';

import useDiscreteApi from '@/hooks/useDiscreteApi';

import checkStatus from '../http/checkStatus';

const { message } = useDiscreteApi();

const CDR = createAxios({
  showErrorMsg: message.error,
  checkStatus,
});

const productApi = useProductApi(CDR);
const opportunityApi = useOpportunityApi(CDR);
const clueApi = useClueApi(CDR);
const customerApi = useCustomerApi(CDR);
const businessApi = useBusinessApi(CDR);
const messageApi = useMessageApi(CDR);
const moduleApi = useModuleApi(CDR);
const orgApi = useOrgApi(CDR);
const roleApi = useRoleApi(CDR);
const loginApi = useLoginApi(CDR);
const sysApi = useSysApi(CDR);
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
  exportOpportunityAll,
  exportOpportunitySelected,
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
  updateClueFollowPlanStatus,
  exportClueAll,
  exportClueSelected,
} = clueApi;

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
  getCustomerTab,
  getCustomerContactTab,
  updateCustomerFollowPlanStatus,
  getCustomerOpportunityPage,
  exportCustomerAll,
  exportCustomerSelected,
} = customerApi;

export const {
  getConfigEmail,
  updateConfigEmail,
  testConfigEmail,
  testConfigSynchronization,
  getConfigSynchronization,
  updateConfigSynchronization,
  getThirdConfigByType,
  getThirdTypeList,
  getAuthList,
  getAuthDetail,
  updateAuth,
  createAuth,
  updateAuthStatus,
  updateAuthName,
  deleteAuth,
  getPersonalInfo,
  updatePersonalInfo,
  sendEmailCode,
  updateUserPassword,
  getPersonalFollow,
  GetRepeatCustomerList,
  GetRepeatClueList,
  GetRepeatClueDetailList,
  GetRepeatOpportunityDetailList,
  exportCenterDownload,
  getExportCenterList,
  cancelCenterExport,
} = businessApi;

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
  updateCapacity,
  addCapacity,
  deleteCapacity,
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
} = moduleApi;

export const {
  getDepartmentTree,
  addDepartment,
  renameDepartment,
  setCommander,
  deleteDepartment,
  addUser,
  updateUser,
  getUserList,
  getUserDetail,
  batchToggleStatusUser,
  batchResetUserPassword,
  resetUserPassword,
  syncOrg,
  batchEditUser,
  importUserPreCheck,
  getUserOptions,
  getRoleOptions,
  importUsers,
  deleteUser,
  deleteUserCheck,
  checkSyncUserFromThird,
  checkDeleteDepartment,
  sortDepartment,
  updateOrgUserName,
  getOrgDepartmentUser,
} = orgApi;

export const {
  relateRoleMember,
  getRoleMember,
  batchRemoveRoleMember,
  updateRole,
  createRole,
  getRoleMemberTree,
  getRoleDeptUserTree,
  getRoleDeptTree,
  removeRoleMember,
  getPermissions,
  getRoles,
  getRoleDetail,
  deleteRole,
  getUsers,
} = roleApi;

export const { login, signout, isLogin, getKey, getWeComCallback, getWeComOauthCallback } = loginApi;

export const { getSystemVersion } = sysApi;

export const { getLicense, addLicense } = licenseApi;
