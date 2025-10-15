import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';

import {
  addAccountPoolView,
  addBusinessView,
  addClueView,
  addContactView,
  addCustomerView,
  addLeadPoolView,
  deleteAccountPoolView,
  deleteBusinessView,
  deleteClueView,
  deleteContactView,
  deleteCustomerView,
  deleteLeadPoolView,
  dragAccountPoolView,
  dragBusinessView,
  dragClueView,
  dragContactView,
  dragCustomerView,
  dragLeadPoolView,
  enableAccountPoolView,
  enableBusinessView,
  enableClueView,
  enableContactView,
  enableCustomerView,
  enableLeadPoolView,
  fixedAccountPoolView,
  fixedBusinessView,
  fixedClueView,
  fixedContactView,
  fixedCustomerView,
  fixedLeadPoolView,
  getAccountPoolViewDetail,
  getAccountPoolViewList,
  getBusinessViewDetail,
  getBusinessViewList,
  getClueViewDetail,
  getClueViewList,
  getContactViewDetail,
  getContactViewList,
  getCustomerViewDetail,
  getCustomerViewList,
  getLeadPoolViewDetail,
  getLeadPoolViewList,
  updateAccountPoolView,
  updateBusinessView,
  updateClueView,
  updateContactView,
  updateCustomerView,
  updateLeadPoolView,
} from '@/api/modules';
import { TabType } from '@/hooks/useHiddenTab';
import useUserStore from '@/store/modules/user';

const userStore = useUserStore();

type ViewAction = 'list' | 'add' | 'update' | 'delete' | 'detail' | 'fixed' | 'enable' | 'drag';

export const viewApiMap: Record<ViewAction, Record<TabType, (...args: any[]) => Promise<any>>> = {
  list: {
    [FormDesignKeyEnum.CLUE]: getClueViewList,
    [FormDesignKeyEnum.CUSTOMER]: getCustomerViewList,
    [FormDesignKeyEnum.CONTACT]: getContactViewList,
    [FormDesignKeyEnum.BUSINESS]: getBusinessViewList,
    [FormDesignKeyEnum.CLUE_POOL]: getLeadPoolViewList,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: getAccountPoolViewList,
    [FormDesignKeyEnum.FOLLOW_PLAN]: async () => ({}),
    [FormDesignKeyEnum.FOLLOW_RECORD]: async () => ({}),
  },
  add: {
    [FormDesignKeyEnum.CLUE]: addClueView,
    [FormDesignKeyEnum.CUSTOMER]: addCustomerView,
    [FormDesignKeyEnum.CONTACT]: addContactView,
    [FormDesignKeyEnum.BUSINESS]: addBusinessView,
    [FormDesignKeyEnum.CLUE_POOL]: addLeadPoolView,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: addAccountPoolView,
    [FormDesignKeyEnum.FOLLOW_PLAN]: async () => ({}),
    [FormDesignKeyEnum.FOLLOW_RECORD]: async () => ({}),
  },
  update: {
    [FormDesignKeyEnum.CLUE]: updateClueView,
    [FormDesignKeyEnum.CUSTOMER]: updateCustomerView,
    [FormDesignKeyEnum.CONTACT]: updateContactView,
    [FormDesignKeyEnum.BUSINESS]: updateBusinessView,
    [FormDesignKeyEnum.CLUE_POOL]: updateLeadPoolView,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: updateAccountPoolView,
    [FormDesignKeyEnum.FOLLOW_PLAN]: async () => ({}),
    [FormDesignKeyEnum.FOLLOW_RECORD]: async () => ({}),
  },
  delete: {
    [FormDesignKeyEnum.CLUE]: deleteClueView,
    [FormDesignKeyEnum.CUSTOMER]: deleteCustomerView,
    [FormDesignKeyEnum.CONTACT]: deleteContactView,
    [FormDesignKeyEnum.BUSINESS]: deleteBusinessView,
    [FormDesignKeyEnum.CLUE_POOL]: deleteLeadPoolView,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: deleteAccountPoolView,
    [FormDesignKeyEnum.FOLLOW_PLAN]: async () => ({}),
    [FormDesignKeyEnum.FOLLOW_RECORD]: async () => ({}),
  },
  detail: {
    [FormDesignKeyEnum.CLUE]: getClueViewDetail,
    [FormDesignKeyEnum.CUSTOMER]: getCustomerViewDetail,
    [FormDesignKeyEnum.CONTACT]: getContactViewDetail,
    [FormDesignKeyEnum.BUSINESS]: getBusinessViewDetail,
    [FormDesignKeyEnum.CLUE_POOL]: getLeadPoolViewDetail,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: getAccountPoolViewDetail,
    [FormDesignKeyEnum.FOLLOW_PLAN]: async () => ({}),
    [FormDesignKeyEnum.FOLLOW_RECORD]: async () => ({}),
  },
  fixed: {
    [FormDesignKeyEnum.CLUE]: fixedClueView,
    [FormDesignKeyEnum.CUSTOMER]: fixedCustomerView,
    [FormDesignKeyEnum.CONTACT]: fixedContactView,
    [FormDesignKeyEnum.BUSINESS]: fixedBusinessView,
    [FormDesignKeyEnum.CLUE_POOL]: fixedLeadPoolView,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: fixedAccountPoolView,
    [FormDesignKeyEnum.FOLLOW_PLAN]: async () => ({}),
    [FormDesignKeyEnum.FOLLOW_RECORD]: async () => ({}),
  },
  enable: {
    [FormDesignKeyEnum.CLUE]: enableClueView,
    [FormDesignKeyEnum.CUSTOMER]: enableCustomerView,
    [FormDesignKeyEnum.CONTACT]: enableContactView,
    [FormDesignKeyEnum.BUSINESS]: enableBusinessView,
    [FormDesignKeyEnum.CLUE_POOL]: enableLeadPoolView,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: enableAccountPoolView,
    [FormDesignKeyEnum.FOLLOW_PLAN]: async () => ({}),
    [FormDesignKeyEnum.FOLLOW_RECORD]: async () => ({}),
  },
  drag: {
    [FormDesignKeyEnum.CLUE]: dragClueView,
    [FormDesignKeyEnum.CUSTOMER]: dragCustomerView,
    [FormDesignKeyEnum.CONTACT]: dragContactView,
    [FormDesignKeyEnum.BUSINESS]: dragBusinessView,
    [FormDesignKeyEnum.CLUE_POOL]: dragLeadPoolView,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: dragAccountPoolView,
    [FormDesignKeyEnum.FOLLOW_PLAN]: async () => ({}),
    [FormDesignKeyEnum.FOLLOW_RECORD]: async () => ({}),
  },
};

export const internalConditionsMap: Record<string, FilterFormItem[]> = {
  [CustomerSearchTypeEnum.ALL]: [
    {
      dataIndex: 'createTime',
      type: FieldTypeEnum.TIME_RANGE_PICKER,
      operator: OperatorEnum.GT,
      value: 0,
    },
  ],
  [CustomerSearchTypeEnum.DEPARTMENT]: [
    {
      dataIndex: 'departmentId',
      type: FieldTypeEnum.TREE_SELECT,
      operator: OperatorEnum.IN,
      value: [userStore.userInfo.departmentId],
      containChildIds: [],
    },
  ],
  [CustomerSearchTypeEnum.SELF]: [
    {
      dataIndex: 'owner',
      type: FieldTypeEnum.MEMBER,
      operator: OperatorEnum.IN,
      value: [userStore.userInfo.id],
      selectedUserList: [{ id: userStore.userInfo.id, name: userStore.userInfo.name }],
    },
  ],
};

export default {};
