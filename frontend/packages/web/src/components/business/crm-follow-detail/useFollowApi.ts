import { ref } from 'vue';
import { useMessage } from 'naive-ui';

import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { CommonList } from '@lib/shared/models/common';
import type { FollowDetailItem } from '@lib/shared/models/customer';

import { cancelClueFollowPlan, getClueFollowPlanList, getClueFollowRecordList } from '@/api/modules/clue/index';
import {
  cancelCustomerFollowPlan,
  getCustomerFollowPlanList,
  getCustomerFollowRecordList,
} from '@/api/modules/customer/index';
import { cancelOptFollowPlan, getOptFollowPlanList, getOptFollowRecordList } from '@/api/modules/opportunity';
import { useI18n } from '@/hooks/useI18n';

export type followEnumType =
  | typeof FormDesignKeyEnum.CUSTOMER
  | typeof FormDesignKeyEnum.BUSINESS
  | typeof FormDesignKeyEnum.CLUE;

type FollowApiMapType = Record<
  followEnumType,
  {
    list: {
      followRecord: (params: any) => Promise<CommonList<FollowDetailItem>>;
      followPlan: (params: any) => Promise<CommonList<FollowDetailItem>>;
    };
    cancel: {
      followPlan: typeof cancelOptFollowPlan;
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
  },
  [FormDesignKeyEnum.CUSTOMER]: {
    list: {
      followRecord: getCustomerFollowRecordList,
      followPlan: getCustomerFollowPlanList,
    },
    cancel: {
      followPlan: cancelCustomerFollowPlan,
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
  },
};

const followFormKeyMap: Record<
  followEnumType,
  {
    followRecord: FormDesignKeyEnum;
    followPlan: FormDesignKeyEnum;
  }
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
  followApiKey: (typeof FormDesignKeyEnum)['CUSTOMER' | 'BUSINESS' | 'CLUE'];
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

  async function loadFollowList() {
    loading.value = true;
    try {
      const params = {
        sourceId: sourceId.value,
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
        keyword: followKeyword.value,
        ...(type.value === 'followPlan' && { status: activeStatus.value }),
      };

      const res = await apis.list[type.value](params);
      data.value = res.list;
      pageNation.value.total = res.total;
    } catch (err) {
      // eslint-disable-next-line no-console
      console.log(err);
    } finally {
      loading.value = false;
    }
  }

  // 取消计划
  async function handleCancelPlan(item: FollowDetailItem) {
    try {
      await apis.cancel.followPlan(item.id);
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

  watch(
    () => type.value,
    (val) => {
      if (['followPlan', 'followRecord'].includes(val)) {
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
    activeStatus,
  };
}
