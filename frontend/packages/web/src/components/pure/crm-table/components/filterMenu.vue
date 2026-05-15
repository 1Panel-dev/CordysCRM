<template>
  <div class="crm-table-filter-menu min-h-[80px]">
    <n-scrollbar content-class="p-[8px] max-w-[160px]" style="max-height: 400px">
      <n-checkbox-group v-model:value="filterConditionMap[props.columnKey]">
        <div
          v-for="(item, index) of displayFilterOptions"
          :key="`${item.value}-${index}`"
          class="crm-table-filter-option"
        >
          <n-tooltip to="body" :delay="300" flip>
            <template #trigger>
              <n-checkbox :value="item.value" label="" class="crm-table-filter-checkbox">
                <div class="one-line-text max-w-[116px]">{{ item.label }}</div>
              </n-checkbox>
            </template>
            {{ item.label }}
          </n-tooltip>
        </div>
      </n-checkbox-group>
    </n-scrollbar>
    <div class="crm-table-filter-footer gap-[8px] p-[8px]">
      <n-button type="default" size="small" class="outline--secondary" @click="handleReset">
        {{ t('common.reset') }}
      </n-button>
      <n-button type="primary" size="small" @click="handleConfirm">
        {{ t('common.confirm') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { DataTableFilterState, NButton, NCheckbox, NCheckboxGroup, NScrollbar, NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import useRemoteFilterOptions from '@/hooks/useRemoteFilterOptions';

  import { FilterOption } from 'naive-ui/es/data-table/src/interface';

  const { t } = useI18n();
  const props = defineProps<{
    columnKey: string;
    filterApiKey?: string;
    filterOptions: FilterOption[];
  }>();

  const emit = defineEmits<{
    (e: 'reset', filters: DataTableFilterState): void;
    (e: 'filter', filters: DataTableFilterState): void;
  }>();

  const filterConditionMap = defineModel<Record<string, (string | number)[]>>('filters', {
    default: {},
  });

  function handleReset() {
    filterConditionMap.value[props.columnKey] = [];
    emit('reset', filterConditionMap.value);
  }

  function handleConfirm() {
    emit('filter', filterConditionMap.value);
  }

  const remoteFilterOptions = computed(() => {
    if (props.filterApiKey) {
      const { filterOptions } = useRemoteFilterOptions(props.filterApiKey);
      return filterOptions;
    }
    return null;
  });

  const displayFilterOptions = computed(() => {
    return props.filterOptions.length ? props.filterOptions : remoteFilterOptions.value?.value;
  });
</script>

<style scoped lang="less">
  .crm-table-filter-menu {
    border-radius: 4px;
    background: var(--text-n10);
    .crm-table-filter-option {
      padding: 4px;
      height: 32px;
      @apply flex items-center;
    }
    :deep(.crm-table-filter-checkbox) {
      @apply flex w-full items-center overflow-hidden;
      .n-checkbox__label {
        @apply flex min-w-0 items-center;

        line-height: 20px;
      }
    }
    .crm-table-filter-footer {
      border-top: 1px solid var(--text-n8);
      border-radius: 2px;
      @apply flex items-center justify-between;
    }
  }
</style>
