import { ref } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type { ContractItem } from '@lib/shared/models/contract';

import type { Description } from '@/components/pure/crm-detail-card/index.vue';

import {
  getAccountContract,
  getAccountContractStatistic,
  getAccountPayment,
  getAccountPaymentRecord,
  getAccountPaymentRecordStatistic,
  getAccountPaymentStatistic,
} from '@/api/modules';

export type TimelineType =
  | FormDesignKeyEnum.CONTRACT_PAYMENT
  | FormDesignKeyEnum.CONTRACT
  | FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD;

export default function useContractTimeline(formKey: TimelineType, sourceId: string) {
  const { t } = useI18n();

  const statisticListMap: Record<TimelineType, any> = {
    [FormDesignKeyEnum.CONTRACT]: [
      {
        key: 'totalAmount',
        label: t('contract.customerContractAmount'),
      },
    ],
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: [
      {
        key: 'totalPlanAmount',
        label: t('contract.customerPaymentAmount'),
      },
    ],
    // TODO lmy 应回款 已回款金额 带回款金额
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: [
      {
        key: 'totalPlanAmount',
        label: t('contract.customerPaymentAmount'),
      },
    ],
  };

  const descriptionListMap: Record<TimelineType, Description[]> = {
    [FormDesignKeyEnum.CONTRACT]: [
      {
        key: 'ownerName',
        label: t('contract.applicant'),
        value: '',
      },
      {
        key: 'name',
        label: t('contract.invoicedContract'),
        value: '',
      },
      {
        key: 'amount',
        label: t('contract.contractAmount'),
        value: '',
      },
      {
        key: 'stage',
        label: t('contract.status'),
        value: '',
      },
      {
        key: 'createTime',
        label: t('contract.applicationTime'),
        value: '',
      },
    ],

    [FormDesignKeyEnum.CONTRACT_PAYMENT]: [
      { key: 'contractName', label: t('contract.paymentContract'), value: '' },
      { key: 'planEndTime', label: t('contract.expectedPaymentDate'), value: '' },
      { key: 'planAmount', label: t('contract.expectedPaymentAmount'), value: '' },
      { key: 'planStatus', label: t('contract.isPlanCompleted'), value: '' },
    ],
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: [
      { key: 'contractName', label: t('contract.paymentContract'), value: '' },
      { key: 'paymentTime', label: t('contract.paymentTime'), value: '' },
      { key: 'paymentAmount', label: t('contract.paymentAmount'), value: '' },
    ],
  };

  const getListApiMap: Record<TimelineType, any> = {
    [FormDesignKeyEnum.CONTRACT]: getAccountContract,
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: getAccountPayment,
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: getAccountPaymentRecord,
  };

  const statisticApiMap: Record<TimelineType, any> = {
    [FormDesignKeyEnum.CONTRACT]: getAccountContractStatistic,
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: getAccountPaymentStatistic,
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: getAccountPaymentRecordStatistic,
  };

  const data = ref<ContractItem[]>([]);
  const loading = ref(false);

  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });

  function getDescription(item: ContractItem) {
    return descriptionListMap[formKey].map((desc) => ({
      ...desc,
      value: item[desc.key as keyof ContractItem],
    })) as Description[];
  }

  async function loadList(refresh = true) {
    try {
      loading.value = true;
      if (refresh) pageNation.value.current = 1;

      const params = {
        customerId: sourceId,
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
      };
      const res = await getListApiMap[formKey](params);

      if (refresh) data.value = [];
      if (res) {
        data.value = data.value.concat(res.list);
        pageNation.value.total = res.total;
      }
    } catch (err) {
      // eslint-disable-next-line no-console
      console.log(err);
    } finally {
      loading.value = false;
    }
  }

  function handleReachBottom() {
    pageNation.value.current += 1;
    if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
      return;
    }
    loadList(false);
  }

  const statisticInfo = ref<Record<string, any>>();
  async function getStatistic() {
    try {
      const res = await statisticApiMap[formKey](sourceId);
      statisticInfo.value = Object.entries(res).map((item) => {
        return {
          key: item[0],
          label: statisticListMap[formKey].find((i: any) => i.key === item[0])?.label,
          value: item[1],
        };
      });
    } catch (err) {
      // eslint-disable-next-line no-console
      console.log(err);
    }
  }

  return {
    data,
    loading,
    statisticInfo,
    getDescription,
    loadList,
    getStatistic,
    handleReachBottom,
  };
}
