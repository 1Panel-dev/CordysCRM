<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :title="t('workbench.duplicateCheck.globalSearchTitle')"
    :footer="false"
    no-padding
    :show-back="true"
  >
    <div class="global-search-wrapper">
      <div class="global-search h-full bg-[var(--text-n10)] p-[16px] pb-0">
        <n-form
          ref="formRef"
          :model="form"
          class="max-w-[60%]"
          label-placement="left"
          require-mark-placement="left"
          :label-width="70"
        >
          <n-form-item path="scoped" :label="t('workbench.duplicateCheck.searchScoped')">
            <n-select
              v-model:value="form.scoped"
              class="w-[300px]"
              :options="scopedOptions"
              :placeholder="t('common.pleaseSelect')"
              @update-value="() => handleReset()"
            />
          </n-form-item>
          <n-form-item path="conditions" :label="t('workbench.duplicateCheck.searchConditions')">
            <FilterContent
              ref="filterContentRef"
              v-model:form-model="formModel"
              :config-list="configList"
              :custom-list="customList"
            />
          </n-form-item>
          <div class="mb-[22px] flex items-center gap-[12px]">
            <n-button type="default" class="outline--secondary" @click="clearFilter">
              {{ t('common.reset') }}
            </n-button>
            <n-button class="mr-[12px]" type="primary" @click="handleFilter">
              {{ t('advanceFilter.filter') }}
            </n-button>
          </div>
        </n-form>
        <div v-if="visible" class="h-full w-full">
          <!-- table TODO -->
          <Suspense>
            <opportunityTable
              ref="opportunityTableRef"
              readonly
              hidden-advance-filter
              :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY"
            />
          </Suspense>
        </div>
      </div>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NForm, NFormItem, NSelect } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { ConditionsItem, FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';
  import opportunityTable from '@/views/opportunity/components/opportunityTable.vue';

  import { scopedOptions } from '../config';

  const { t } = useI18n();

  const props = defineProps<{
    formKey: FormDesignKeyEnum | null;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const defaultFormModel: FilterForm = {
    searchMode: 'AND',
    list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
  };

  const formModel = ref<FilterForm>(cloneDeep(defaultFormModel));
  const savedFormModel = ref(cloneDeep(formModel.value));
  const filterContentRef = ref<InstanceType<typeof FilterContent>>();

  const isAdvancedSearchMode = ref(false);
  const filterResult = ref<FilterResult>({ searchMode: 'AND', conditions: [] });

  const form = ref<{
    scoped: null | FormDesignKeyEnum;
  }>({
    scoped: null,
  });

  const opportunityTableRef = ref<InstanceType<typeof opportunityTable>>();

  const configList = computed<FilterFormItem[]>(() => {
    if (props.formKey === FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY) {
      return (opportunityTableRef.value?.originFilterConfigList ?? []) as FilterFormItem[];
    }
    return [];
  });

  const customList = computed<FilterFormItem[]>(() => {
    if (props.formKey === FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY) {
      return (opportunityTableRef.value?.originCustomFieldsFilterConfig ?? []) as FilterFormItem[];
    }
    return [];
  });

  function getParams(): FilterResult {
    const conditions: ConditionsItem[] = formModel.value.list.map((item: any) => ({
      value: item.value,
      operator: item.operator,
      name: item.dataIndex ?? '',
      multipleValue: multipleValueTypeList.includes(item.type),
      type: item.type,
    }));

    return {
      searchMode: formModel.value.searchMode,
      conditions,
    };
  }

  const getIsValidValue = (item: ConditionsItem) => {
    if (typeof item.value === 'boolean') return String(item.value).length;
    if (typeof item.value === 'number') return item.value;
    return item.value?.length;
  };

  const handleFilterConditions = (filter: FilterResult) => {
    const haveConditions: boolean =
      filter.conditions?.some((item) => {
        const valueCanEmpty = ['EMPTY', 'NOT_EMPTY'].includes(item.operator as string);
        return valueCanEmpty || getIsValidValue(item);
      }) ?? false;

    isAdvancedSearchMode.value = haveConditions;
    filterResult.value = filter;
    // TODO xinxinwu
    if (opportunityTableRef.value?.setAdvanceFilterParams) {
      opportunityTableRef.value?.setAdvanceFilterParams(filter);
    }
  };

  function handleFilter() {
    filterContentRef.value?.formRef?.validate((errors) => {
      if (!errors) {
        handleFilterConditions(getParams());
      }
    });
  }

  function handleReset() {
    filterContentRef.value?.formRef?.restoreValidation();
    formModel.value = JSON.parse(JSON.stringify(savedFormModel.value));
  }

  function clearFilter() {
    handleReset();
    handleFilterConditions({ searchMode: 'AND', conditions: [] });
  }

  watch(
    () => props.formKey,
    (val) => {
      form.value.scoped = val;
    }
  );
</script>

<style scoped lang="less">
  .global-search-wrapper {
    padding: 16px;
    background: var(--text-n9);
    @apply h-full w-full;
    .global-search {
      @apply overflow-y-auto;
      .crm-scroll-bar();
    }
  }
</style>
