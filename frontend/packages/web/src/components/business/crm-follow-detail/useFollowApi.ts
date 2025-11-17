import { ref } from 'vue';
import { useMessage } from 'naive-ui';

import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { sleep } from '@lib/shared/method';
import type { CommonList } from '@lib/shared/models/common';
import type { CustomerFollowPlanListItem, FollowDetailItem } from '@lib/shared/models/customer';

import {
  deleteClueFollowPlan,
  deleteClueFollowRecord,
  deleteCustomerFollowPlan,
  deleteCustomerFollowRecord,
  deleteOptFollowPlan,
  deleteOptFollowRecord,
  getClueFollowPlanList,
  getClueFollowRecordList,
  getCluePoolFollowRecordList,
  getCustomerFollowPlanList,
  getCustomerFollowRecordList,
  getCustomerOpenSeaFollowRecordList,
  getOptFollowPlanList,
  getOptFollowRecordList,
  getPersonalFollow,
  updateClueFollowPlanStatus,
  updateCustomerFollowPlanStatus,
  updateOptFollowPlanStatus,
} from '@/api/modules';

import useHighlight from './useHighlight';

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
    changeStatus?: {
      followPlan: typeof updateOptFollowPlanStatus;
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
    changeStatus: {
      followPlan: updateOptFollowPlanStatus,
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
    changeStatus: {
      followPlan: updateCustomerFollowPlanStatus,
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
    changeStatus: {
      followPlan: updateClueFollowPlanStatus,
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
      followRecord: getCustomerOpenSeaFollowRecordList,
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

  function transformField(item: FollowDetailItem, optionMap?: Record<string, any>) {
    const methodKey = type.value === 'followPlan' ? 'method' : 'followMethod';
    let followMethod;
    if (optionMap) {
      followMethod =
        optionMap[methodKey]?.find((e: any) => e.id === item[methodKey as keyof FollowDetailItem])?.name || '-';
    }
    return {
      ...item,
      [methodKey]: followMethod,
    };
  }

  async function loadFollowList(refresh = true) {
    try {
      loading.value = true;
      if (refresh) {
        pageNation.value.current = 1;
      }
      const params = {
        sourceId: sourceId.value,
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
        keyword: followKeyword.value,
        ...(type.value === 'followPlan' && { status: activeStatus.value }),
        myPlan: followApiKey === 'myPlan',
      };
      const res = await apis.list[type.value]?.(params);
      if (refresh) {
        data.value = [];
      }
      if (res) {
        const newList = res.list.map((item: FollowDetailItem) => transformField(item, res?.optionMap));
        data.value = data.value.concat(newList);
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
        return FormDesignKeyEnum.BUSINESS;
      }
      return FormDesignKeyEnum.CUSTOMER;
    }
    return followApiKey;
  }

  // 更新计划状态
  async function changePlanStatus(item: FollowDetailItem) {
    try {
      await followApiMap[getApiKey(item)].changeStatus?.followPlan({
        id: item.id,
        status: (item as CustomerFollowPlanListItem).status,
      });
      Message.success(t('common.operationSuccess'));
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
    loadFollowList(false);
  }

  const { highlightContent, resetHighlight } = useHighlight([
    '.crm-follow-record-content',
    '.crm-follow-record-method',
  ]);

  async function searchData(keyword: string) {
    followKeyword.value = keyword;
    await loadFollowList();
    await sleep(300);

    highlightContent(keyword);
    if (!keyword) {
      resetHighlight();
    }
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

  watch(
    () => type.value,
    (val) => {
      if (['followPlan', 'followRecord'].includes(val)) {
        loadFollowList(true);
      }
    }
  );

  return {
    data,
    loading,
    handleReachBottom,
    followKeyword,
    loadFollowList,
    changePlanStatus,
    followFormKeyMap,
    searchData,
    handleDelete,
    activeStatus,
    getApiKey,
  };
}
