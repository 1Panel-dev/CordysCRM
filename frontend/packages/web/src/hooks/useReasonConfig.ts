import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';

import { getReasonConfig } from '@/api/modules';

import { FilterOption } from 'naive-ui/es/data-table/src/interface';

function getReasonKey(formKey: FormDesignKeyEnum) {
  switch (formKey) {
    case FormDesignKeyEnum.CUSTOMER_OPEN_SEA:
    case FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC:
      return ReasonTypeEnum.CUSTOMER_POOL_RS;
    case FormDesignKeyEnum.CLUE_POOL:
    case FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL:
      return ReasonTypeEnum.CLUE_POOL_RS;
    case FormDesignKeyEnum.BUSINESS:
    case FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY:
      return ReasonTypeEnum.OPPORTUNITY_FAIL_RS;
    default:
      break;
  }
}

export default function useReasonConfig(formKey: FormDesignKeyEnum) {
  const reasonOptions = ref<FilterOption[]>([]);
  const reasonKey = ref();
  reasonKey.value = getReasonKey(formKey);

  async function initReasonConfig() {
    if (!reasonKey.value) return;
    try {
      const { dictList } = await getReasonConfig(reasonKey.value);
      reasonOptions.value = dictList.map((e) => ({ label: e.name, value: e.id }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  return {
    initReasonConfig,
    reasonOptions,
  };
}
