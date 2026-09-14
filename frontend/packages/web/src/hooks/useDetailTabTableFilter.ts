import { type MaybeRef, unref } from 'vue';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import type { FilterResult } from '@/components/pure/crm-advance-filter/type';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

export interface DetailTabFilter {
  fieldId: string;
  sourceId: string;
}

export default function useDetailTabTableFilter() {
  function applyDetailTabFilter(
    detailTabFilter: DetailTabFilter | undefined,
    fieldList: MaybeRef<FormCreateField[]>,
    setAdvanceFilter: (filter: FilterResult) => void
  ) {
    if (!detailTabFilter) {
      return;
    }

    const field = unref(fieldList).find((item) => item.id === detailTabFilter.fieldId);
    if (!field || ![FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type)) {
      return;
    }

    setAdvanceFilter({
      searchMode: 'AND',
      conditions: [
        {
          name: field.businessKey || field.id,
          value: [detailTabFilter.sourceId],
          operator: OperatorEnum.IN,
          multipleValue: field.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE,
          type: field.type,
        },
      ],
    });
  }

  return {
    applyDetailTabFilter,
  };
}
