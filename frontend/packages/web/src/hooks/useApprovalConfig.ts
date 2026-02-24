import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';

import { getReasonConfig } from '@/api/modules';

import { FilterOption } from 'naive-ui/es/data-table/src/interface';

export type ApprovalConfigType =
  | FormDesignKeyEnum.CONTRACT
  | FormDesignKeyEnum.INVOICE
  | FormDesignKeyEnum.OPPORTUNITY_QUOTATION;

const dicApprovalKeyMap: Record<ApprovalConfigType, ReasonTypeEnum> = {
  [FormDesignKeyEnum.CONTRACT]: ReasonTypeEnum.CONTRACT_APPROVAL,
  [FormDesignKeyEnum.INVOICE]: ReasonTypeEnum.INVOICE_APPROVAL,
  [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: ReasonTypeEnum.QUOTATION_APPROVAL,
};

export default function useApprovalConfig(formKey: FormDesignKeyEnum) {
  const reasonOptions = ref<FilterOption[]>([]);
  const dicApprovalEnable = ref(false);
  const dicKey = ref();
  dicKey.value = dicApprovalKeyMap[formKey as keyof typeof dicApprovalKeyMap];

  async function initApprovalConfig() {
    if (!dicKey.value) return;
    try {
      const { dictList, enable } = await getReasonConfig(dicKey.value);
      reasonOptions.value = dictList.map((e) => ({ label: e.name, value: e.id }));
      dicApprovalEnable.value = enable;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  return {
    initApprovalConfig,
    dicApprovalEnable,
  };
}
