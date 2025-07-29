import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';

import { getReasonConfig } from '@/api/modules';

import { FilterOption } from 'naive-ui/es/data-table/src/interface';

function getReasonKey(formKey: FormDesignKeyEnum) {
  switch (formKey) {
    case FormDesignKeyEnum.CUSTOMER:
      return ReasonTypeEnum.CUSTOMER_POOL_RS;
    case FormDesignKeyEnum.CLUE:
      return ReasonTypeEnum.CLUE_POOL_RS;
    case FormDesignKeyEnum.BUSINESS:
      return ReasonTypeEnum.OPPORTUNITY_FAIL_RS;
    default:
      break;
  }
}

// TODO 权限还没有限制，需要和后台确认 xxw
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
