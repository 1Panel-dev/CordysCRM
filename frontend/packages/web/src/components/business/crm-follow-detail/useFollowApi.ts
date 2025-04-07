import { ref } from 'vue';
import { useMessage } from 'naive-ui';

import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { CommonList } from '@lib/shared/models/common';
import type { FollowDetailItem } from '@lib/shared/models/customer';

import type { FormCreateField } from '@/components/business/crm-form-create/types';

import {
  cancelClueFollowPlan,
  deleteClueFollowPlan,
  deleteClueFollowRecord,
  getClueFollowPlanList,
  getClueFollowRecordList,
  getCluePoolFollowRecordList,
} from '@/api/modules/clue/index';
import {
  cancelCustomerFollowPlan,
  deleteCustomerFollowPlan,
  deleteCustomerFollowRecord,
  getCustomerFollowPlanFormConfig,
  getCustomerFollowPlanList,
  getCustomerFollowRecordFormConfig,
  getCustomerFollowRecordList,
  GetCustomerOpenSeaFollowRecordList,
} from '@/api/modules/customer/index';
import {
  cancelOptFollowPlan,
  deleteOptFollowPlan,
  deleteOptFollowRecord,
  getOptFollowPlanList,
  getOptFollowRecordList,
} from '@/api/modules/opportunity';
import { getPersonalFollow } from '@/api/modules/system/business';
import { useI18n } from '@/hooks/useI18n';

export type followEnumType =
  | typeof FormDesignKeyEnum.CUSTOMER
  | typeof FormDesignKeyEnum.BUSINESS
  | typeof FormDesignKeyEnum.CLUE
  | typeof FormDesignKeyEnum.CLUE_POOL
  | typeof FormDesignKeyEnum.CUSTOMER_OPEN_SEA
  | 'myPlan';

type FollowApiMapType = Record<
  followEnumType,
  {
    list: {
      followRecord?: (params: any) => Promise<CommonList<FollowDetailItem>>;
      followPlan?: (params: any) => Promise<CommonList<FollowDetailItem>>;
    };
    cancel?: {
      followPlan: typeof cancelOptFollowPlan;
    };
    delete?: {
      followRecord: (params: string) => Promise<any>;
      followPlan?: (params: string) => Promise<any>;
    };
  }
>;

const followApiMap: FollowApiMapType = {
  [FormDesignKeyEnum.BUSINESS]: {
    list: {
      followRecord: getOptFollowRecordList,
      followPlan: getOptFollowPlanList,
    },
    cancel: {
      followPlan: cancelOptFollowPlan,
    },
    delete: {
      followRecord: deleteOptFollowRecord,
      followPlan: deleteOptFollowPlan,
    },
  },
  [FormDesignKeyEnum.CUSTOMER]: {
    list: {
      followRecord: getCustomerFollowRecordList,
      followPlan: getCustomerFollowPlanList,
    },
    cancel: {
      followPlan: cancelCustomerFollowPlan,
    },
    delete: {
      followRecord: deleteCustomerFollowRecord,
      followPlan: deleteCustomerFollowPlan,
    },
  },
  [FormDesignKeyEnum.CLUE]: {
    list: {
      followRecord: getClueFollowRecordList,
      followPlan: getClueFollowPlanList,
    },
    cancel: {
      followPlan: cancelClueFollowPlan,
    },
    delete: {
      followRecord: deleteClueFollowRecord,
      followPlan: deleteClueFollowPlan,
    },
  },
  [FormDesignKeyEnum.CLUE_POOL]: {
    list: {
      followRecord: getCluePoolFollowRecordList,
    },
  },
  [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: {
    list: {
      followRecord: GetCustomerOpenSeaFollowRecordList,
    },
  },
  myPlan: {
    list: {
      followPlan: getPersonalFollow,
    },
  },
};

const followFormKeyMap: Partial<
  Record<
    followEnumType,
    {
      followRecord: FormDesignKeyEnum;
      followPlan: FormDesignKeyEnum;
    }
  >
> = {
  [FormDesignKeyEnum.CUSTOMER]: {
    followRecord: FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER, // 客户跟进记录
    followPlan: FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER, // 客户跟进计划
  },
  [FormDesignKeyEnum.BUSINESS]: {
    followRecord: FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS, // 商机跟进记录
    followPlan: FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS, // 商机跟进计划
  },
  [FormDesignKeyEnum.CLUE]: {
    followRecord: FormDesignKeyEnum.FOLLOW_RECORD_CLUE, // 线索跟进记录
    followPlan: FormDesignKeyEnum.FOLLOW_PLAN_CLUE, // 线索跟进计划
  },
};

export default function useFollowApi(followProps: {
  followApiKey: followEnumType;
  type: Ref<'followRecord' | 'followPlan'>;
  sourceId: Ref<string>;
}) {
  const { t } = useI18n();

  const Message = useMessage();

  const data = ref<FollowDetailItem[]>([]);

  const activeStatus = ref<CustomerFollowPlanStatusEnum>(CustomerFollowPlanStatusEnum.ALL);

  const followKeyword = ref<string>('');

  const loading = ref(false);

  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });
  const { type, followApiKey, sourceId } = followProps;

  const apis = followApiMap[followApiKey];

  const fieldList = ref<FormCreateField[]>([]);

  function transformField(e: FollowDetailItem) {
    const tmpObject: Record<string, any> = {};
    (e.moduleFields || []).forEach((moduleField) => {
      const fieldVal = fieldList.value.find((field) => moduleField.fieldId === field.id);
      if (fieldVal) {
        const isSelectableField = [FieldTypeEnum.SELECT, FieldTypeEnum.CHECKBOX, FieldTypeEnum.RADIO].includes(
          fieldVal.type
        );
        if (isSelectableField && fieldVal.options?.length) {
          const option = fieldVal.options.find((item) => item.value === moduleField.fieldValue);
          tmpObject[fieldVal.internalKey as string] = option ? option.label : '-';
        } else {
          tmpObject[fieldVal.internalKey as string] = moduleField.fieldValue;
        }
      }
    });
    return {
      ...e,
      ...tmpObject,
    };
  }

  async function loadFollowList() {
    loading.value = true;
    try {
      const params = {
        sourceId: sourceId.value,
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
        keyword: followKeyword.value,
        ...(type.value === 'followPlan' && { status: activeStatus.value }),
        myPlan: followApiKey === 'myPlan',
      };
      const res = await apis.list[type.value]?.(params);
      if (res) {
        data.value = res.list.map((item) => transformField(item));
        pageNation.value.total = res.total;
      }
    } catch (err) {
      // eslint-disable-next-line no-console
      console.log(err);
    } finally {
      loading.value = false;
    }
  }

  function getApiKey(item: FollowDetailItem) {
    if (followApiKey === 'myPlan') {
      if (item.clueId?.length) {
        return FormDesignKeyEnum.CLUE;
      }
      if (item.opportunityId?.length && item.customerId?.length) {
        return FormDesignKeyEnum.CUSTOMER;
      }
      return FormDesignKeyEnum.BUSINESS;
    }
    return followApiKey;
  }

  // 取消计划
  async function handleCancelPlan(item: FollowDetailItem) {
    try {
      await followApiMap[getApiKey(item)].cancel?.followPlan(item.id);
      Message.success(t('common.cancelSuccess'));
      loadFollowList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleReachBottom() {
    pageNation.value.current += 1;
    if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
      return;
    }
    loadFollowList();
  }

  function searchData(keyword: string) {
    followKeyword.value = keyword;
    loadFollowList();
  }

  // 删除
  async function handleDelete(item: FollowDetailItem) {
    try {
      await followApiMap[getApiKey(item)].delete?.[type.value]?.(item.id);
      Message.success(t('common.deleteSuccess'));
      loadFollowList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function initFollowFormConfig() {
    try {
      let res;
      if (type.value === 'followRecord') {
        res = await getCustomerFollowRecordFormConfig();
      } else {
        res = await getCustomerFollowPlanFormConfig();
      }
      fieldList.value = res.fields;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => type.value,
    (val) => {
      if (['followPlan', 'followRecord'].includes(val)) {
        initFollowFormConfig();
        loadFollowList();
      }
    }
  );

  return {
    data,
    loading,
    handleReachBottom,
    followKeyword,
    loadFollowList,
    handleCancelPlan,
    followFormKeyMap,
    searchData,
    handleDelete,
    activeStatus,
    getApiKey,
    initFollowFormConfig,
  };
}
