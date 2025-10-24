<template>
  <div class="flex flex-row items-center gap-[8px]">
    <CrmSearchInput
      v-if="!props.notShowInputSearch && !isAdvancedSearchMode"
      v-model:value="keyword"
      class="!w-[240px]"
      :placeholder="props.searchPlaceholder"
      @search="emit('keywordSearch', $event)"
    />
    <n-button
      :type="isAdvancedSearchMode ? 'primary' : 'default'"
      :ghost="isAdvancedSearchMode"
      class="outline--secondary px-[8px]"
      @click="handleOpenFilter"
    >
      <CrmIcon type="iconicon_filter" :size="16" />
    </n-button>
    <n-button v-show="isAdvancedSearchMode" text type="primary" @click="clearFilter">
      {{ t('advanceFilter.clearFilter') }}
    </n-button>
  </div>

  <FilterModal
    ref="filterModalRef"
    v-model:visible="visible"
    :config-list="props.filterConfigList"
    :custom-list="props.customFieldsConfigList"
    @handle-filter="handleFilter"
    @reset="() => clearFilter()"
  />
</template>

<script lang="ts" setup>
  import { NButton } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import FilterModal from './filterModal.vue';

  import { ConditionsItem, FilterForm, FilterFormItem, FilterResult } from './type';

  const { t } = useI18n();

  const props = defineProps<{
    filterConfigList: FilterFormItem[]; // 系统字段
    customFieldsConfigList?: FilterFormItem[]; // 自定义字段
    notShowInputSearch?: boolean;
    searchPlaceholder?: string;
  }>();

  const emit = defineEmits<{
    (e: 'keywordSearch', value: string): void;
    (e: 'advSearch', value: FilterResult, isAdvancedSearchMode: boolean, originalForm?: FilterForm): void;
    (e: 'refresh', value: FilterResult): void;
  }>();

  const keyword = defineModel<string>('keyword', { default: '' });

  const visible = ref(false);
  const isAdvancedSearchMode = ref(false);

  const filterResult = ref<FilterResult>({ searchMode: 'AND', conditions: [] });

  function handleOpenFilter() {
    visible.value = !visible.value;
  }

  const getIsValidValue = (item: ConditionsItem) => {
    if (typeof item.value === 'boolean') return String(item.value).length;
    if (typeof item.value === 'number') return item.value;
    return item.value?.length;
  };

  const handleFilter = (filter: FilterResult, originalForm?: FilterForm) => {
    keyword.value = '';
    const haveConditions: boolean =
      filter.conditions?.some((item) => {
        const valueCanEmpty = ['EMPTY', 'NOT_EMPTY'].includes(item.operator as string);
        return valueCanEmpty || getIsValidValue(item);
      }) ?? false;

    isAdvancedSearchMode.value = haveConditions;
    filterResult.value = filter;
    emit('advSearch', filter, isAdvancedSearchMode.value, originalForm);
  };

  const filterModalRef = ref<InstanceType<typeof FilterModal>>();
  function clearFilter() {
    filterModalRef.value?.handleReset();
    handleFilter({ searchMode: 'AND', conditions: [] });
  }

  function setAdvancedFilter(filter: FilterResult, isAdvancedMode: boolean) {
    filterModalRef.value?.setFormModal(filter);
    isAdvancedSearchMode.value = isAdvancedMode;
  }

  defineExpose({
    clearFilter,
    isAdvancedSearchMode,
    setAdvancedFilter,
  });
</script>

<style lang="less" scoped></style>
