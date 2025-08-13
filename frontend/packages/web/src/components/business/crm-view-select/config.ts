import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { OpportunitySearchTypeEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';

import {
  addBusinessView,
  addClueView,
  addContactView,
  addCustomerView,
  deleteBusinessView,
  deleteClueView,
  deleteContactView,
  deleteCustomerView,
  dragBusinessView,
  dragClueView,
  dragContactView,
  dragCustomerView,
  enableBusinessView,
  enableClueView,
  enableContactView,
  enableCustomerView,
  fixedBusinessView,
  fixedClueView,
  fixedContactView,
  fixedCustomerView,
  getBusinessViewDetail,
  getBusinessViewList,
  getClueViewDetail,
  getClueViewList,
  getContactViewDetail,
  getContactViewList,
  getCustomerViewDetail,
  getCustomerViewList,
  updateBusinessView,
  updateClueView,
  updateContactView,
  updateCustomerView,
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
  },
  add: {
    [FormDesignKeyEnum.CLUE]: addClueView,
    [FormDesignKeyEnum.CUSTOMER]: addCustomerView,
    [FormDesignKeyEnum.CONTACT]: addContactView,
    [FormDesignKeyEnum.BUSINESS]: addBusinessView,
  },
  update: {
    [FormDesignKeyEnum.CLUE]: updateClueView,
    [FormDesignKeyEnum.CUSTOMER]: updateCustomerView,
    [FormDesignKeyEnum.CONTACT]: updateContactView,
    [FormDesignKeyEnum.BUSINESS]: updateBusinessView,
  },
  delete: {
    [FormDesignKeyEnum.CLUE]: deleteClueView,
    [FormDesignKeyEnum.CUSTOMER]: deleteCustomerView,
    [FormDesignKeyEnum.CONTACT]: deleteContactView,
    [FormDesignKeyEnum.BUSINESS]: deleteBusinessView,
  },
  detail: {
    [FormDesignKeyEnum.CLUE]: getClueViewDetail,
    [FormDesignKeyEnum.CUSTOMER]: getCustomerViewDetail,
    [FormDesignKeyEnum.CONTACT]: getContactViewDetail,
    [FormDesignKeyEnum.BUSINESS]: getBusinessViewDetail,
  },
  fixed: {
    [FormDesignKeyEnum.CLUE]: fixedClueView,
    [FormDesignKeyEnum.CUSTOMER]: fixedCustomerView,
    [FormDesignKeyEnum.CONTACT]: fixedContactView,
    [FormDesignKeyEnum.BUSINESS]: fixedBusinessView,
  },
  enable: {
    [FormDesignKeyEnum.CLUE]: enableClueView,
    [FormDesignKeyEnum.CUSTOMER]: enableCustomerView,
    [FormDesignKeyEnum.CONTACT]: enableContactView,
    [FormDesignKeyEnum.BUSINESS]: enableBusinessView,
  },
  drag: {
    [FormDesignKeyEnum.CLUE]: dragClueView,
    [FormDesignKeyEnum.CUSTOMER]: dragCustomerView,
    [FormDesignKeyEnum.CONTACT]: dragContactView,
    [FormDesignKeyEnum.BUSINESS]: dragBusinessView,
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
  [OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS]: [
    {
      dataIndex: 'stage',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      operator: OperatorEnum.IN,
      value: [StageResultEnum.SUCCESS],
    },
  ],
};

export default {};
