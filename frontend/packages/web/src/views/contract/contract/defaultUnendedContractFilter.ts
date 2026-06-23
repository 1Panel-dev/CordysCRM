import type { FilterConditionItem } from '@lib/shared/models/common';

import type { ConditionsItem, FilterResult } from '@/components/pure/crm-advance-filter/type';

export const CONTRACT_END_TIME_FIELD = 'endTime';

type ContractEndTimeFieldCarrier = {
  name?: string;
  dataIndex?: string | null;
};

function isContractEndTimeField(item?: ContractEndTimeFieldCarrier) {
  return item?.name === CONTRACT_END_TIME_FIELD || item?.dataIndex === CONTRACT_END_TIME_FIELD;
}

export function hasContractEndTimeCondition(filter?: FilterResult) {
  return filter?.conditions?.some(isContractEndTimeField) ?? false;
}

export function withDefaultUnendedContractFilter(filter?: FilterResult, now = Date.now()): FilterResult {
  const conditions = filter?.conditions ?? [];

  if (hasContractEndTimeCondition(filter)) {
    return {
      searchMode: filter?.searchMode ?? 'AND',
      conditions,
    };
  }

  const defaultCondition: ConditionsItem = {
    name: CONTRACT_END_TIME_FIELD,
    value: now,
    operator: 'GE' as ConditionsItem['operator'],
    multipleValue: false,
    type: 'DATE_TIME' as ConditionsItem['type'],
  };

  return {
    searchMode: filter?.searchMode ?? 'AND',
    conditions: [...conditions, defaultCondition],
  };
}

export function hasContractEndTimeFilter(filters?: ContractEndTimeFieldCarrier[]) {
  return filters?.some(isContractEndTimeField) ?? false;
}

export function withDefaultUnendedContractFilters(filters: FilterConditionItem[] = [], now = Date.now()) {
  if (hasContractEndTimeFilter(filters)) {
    return filters;
  }

  return [
    ...filters,
    {
      name: CONTRACT_END_TIME_FIELD,
      value: now,
      operator: 'GE' as FilterConditionItem['operator'],
      multipleValue: false,
    },
  ];
}
