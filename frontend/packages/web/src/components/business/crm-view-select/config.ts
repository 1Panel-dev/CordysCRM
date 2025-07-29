import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';

import {
  addBusinessView,
  deleteBusinessView,
  enableBusinessView,
  fixedBusinessView,
  getBusinessViewDetail,
  getBusinessViewList,
  updateBusinessView,
} from '@/api/modules';
import { TabType } from '@/hooks/useHiddenTab';
import useUserStore from '@/store/modules/user';

const userStore = useUserStore();

type ViewAction = 'list' | 'add' | 'update' | 'delete' | 'detail' | 'fixed' | 'enable';

// TODO lmy 联调
export const viewApiMap: Record<ViewAction, Record<TabType, (...args: any[]) => Promise<any>>> = {
  list: {
    [FormDesignKeyEnum.CLUE]: getBusinessViewList,
    [FormDesignKeyEnum.CUSTOMER]: getBusinessViewList,
    [FormDesignKeyEnum.CONTACT]: getBusinessViewList,
    [FormDesignKeyEnum.BUSINESS]: getBusinessViewList,
  },
  add: {
    [FormDesignKeyEnum.CLUE]: addBusinessView,
    [FormDesignKeyEnum.CUSTOMER]: addBusinessView,
    [FormDesignKeyEnum.CONTACT]: addBusinessView,
    [FormDesignKeyEnum.BUSINESS]: addBusinessView,
  },
  update: {
    [FormDesignKeyEnum.CLUE]: updateBusinessView,
    [FormDesignKeyEnum.CUSTOMER]: updateBusinessView,
    [FormDesignKeyEnum.CONTACT]: updateBusinessView,
    [FormDesignKeyEnum.BUSINESS]: updateBusinessView,
  },
  delete: {
    [FormDesignKeyEnum.CLUE]: deleteBusinessView,
    [FormDesignKeyEnum.CUSTOMER]: deleteBusinessView,
    [FormDesignKeyEnum.CONTACT]: deleteBusinessView,
    [FormDesignKeyEnum.BUSINESS]: deleteBusinessView,
  },
  detail: {
    [FormDesignKeyEnum.CLUE]: getBusinessViewDetail,
    [FormDesignKeyEnum.CUSTOMER]: getBusinessViewDetail,
    [FormDesignKeyEnum.CONTACT]: getBusinessViewDetail,
    [FormDesignKeyEnum.BUSINESS]: getBusinessViewDetail,
  },
  fixed: {
    [FormDesignKeyEnum.CLUE]: fixedBusinessView,
    [FormDesignKeyEnum.CUSTOMER]: fixedBusinessView,
    [FormDesignKeyEnum.CONTACT]: fixedBusinessView,
    [FormDesignKeyEnum.BUSINESS]: fixedBusinessView,
  },
  enable: {
    [FormDesignKeyEnum.CLUE]: enableBusinessView,
    [FormDesignKeyEnum.CUSTOMER]: enableBusinessView,
    [FormDesignKeyEnum.CONTACT]: enableBusinessView,
    [FormDesignKeyEnum.BUSINESS]: enableBusinessView,
  },
};

// TODO lmy
export const internalConditionsMap: Record<string, FilterFormItem[]> = {
  [CustomerSearchTypeEnum.ALL]: [
    {
      dataIndex: 'createTime',
      type: FieldTypeEnum.TIME_RANGE_PICKER,
      operator: OperatorEnum.GT,
      value: '0',
    },
  ],
  [CustomerSearchTypeEnum.DEPARTMENT]: [
    {
      dataIndex: 'departmentId',
      type: FieldTypeEnum.TREE_SELECT,
      operator: OperatorEnum.IN,
      value: [userStore.userInfo.departmentId],
    },
  ],
  [CustomerSearchTypeEnum.SELF]: [
    {
      dataIndex: 'createUser',
      type: FieldTypeEnum.USER_SELECT,
      operator: OperatorEnum.IN,
      value: [userStore.userInfo.name],
    },
  ],
  [OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS]: [],
};

export default {};
