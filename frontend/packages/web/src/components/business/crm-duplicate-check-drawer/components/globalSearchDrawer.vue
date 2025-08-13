<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :title="t('workbench.duplicateCheck.searchInCordys')"
    :footer="false"
    no-padding
    :show-back="true"
    @cancel="handleCancel"
  >
    <div class="global-search-wrapper">
      <div class="global-search">
        <CrmCard hide-footer auto-height class="mb-[16px]">
          <div class="w-full bg-[var(--text-n10)]">
            <div class="mb-[16px] flex w-full items-center gap-[12px]">
              <div class="flex flex-nowrap">{{ t('workbench.duplicateCheck.searchScoped') }}</div>
              <div>
                <CrmTab
                  v-model:active-tab="activeTab"
                  no-content
                  :tab-list="tabList"
                  type="segment"
                  @change="() => handleReset()"
                />
              </div>
            </div>
            <div class="w-[50%]">
              <FilterContent
                ref="filterContentRef"
                v-model:form-model="formModel"
                :config-list="configList"
                :custom-list="customList"
                :max-filter-field-number="5"
                keep-one-line
                :max-filter-field-add-tooltip="t('workbench.duplicateCheck.maxConditionTooltip')"
              >
                <template #addButtonRight>
                  <div class="flex items-center gap-[12px]">
                    <n-button size="small" type="default" class="outline--secondary" @click="clearFilter">
                      {{ t('common.reset') }}
                    </n-button>
                    <n-button size="small" type="primary" @click="handleFilter">
                      {{ t('advanceFilter.filter') }}
                    </n-button>
                  </div>
                </template>
              </FilterContent>
            </div>
          </div>
        </CrmCard>
        <CrmCard hide-footer :special-height="totalCount > 0 ? 0 : 228">
          <Suspense>
            <div v-if="activeTab === FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY" class="h-full w-full">
              <opportunityTable
                ref="opportunityTableRef"
                readonly
                hidden-total
                is-limit-show-detail
                hidden-advance-filter
                :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY"
                @init="setFilterConfigList"
              >
                <template #searchTableTotal="{ total }">
                  <globalSearchResult :title="currentTitle" :total="total" @init-total="initTotal" />
                </template>
              </opportunityTable>
            </div>

            <customerTable
              v-else-if="activeTab === FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER"
              ref="customerTableRef"
              readonly
              hidden-total
              is-limit-show-detail
              hidden-advance-filter
              :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER"
              @init="setFilterConfigList"
              @show-count-detail="(row, type) => emit('showCountDetail', row, type)"
            >
              <template #searchTableTotal="{ total }">
                <globalSearchResult :title="currentTitle" :total="total" @init-total="initTotal" />
              </template>
            </customerTable>
            <ContactTable
              v-else-if="activeTab === FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT"
              ref="contactTableRef"
              readonly
              hidden-total
              hidden-advance-filter
              :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT"
              @init="setFilterConfigList"
            >
              <template #searchTableTotal="{ total }">
                <globalSearchResult :title="currentTitle" :total="total" @init-total="initTotal" />
              </template>
            </ContactTable>
            <openSeaTable
              v-else-if="activeTab === FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC"
              ref="openSeaTableRef"
              readonly
              hidden-total
              hidden-pool-select
              hidden-advance-filter
              :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC"
              @init="setFilterConfigList"
            >
              <template #searchTableTotal="{ total }">
                <globalSearchResult :title="currentTitle" :total="total" @init-total="initTotal" />
              </template>
            </openSeaTable>
            <clueTable
              v-else-if="activeTab === FormDesignKeyEnum.SEARCH_GLOBAL_CLUE"
              ref="clueTableRef"
              readonly
              hidden-total
              is-limit-show-detail
              hidden-advance-filter
              :table-form-key="FormDesignKeyEnum.SEARCH_GLOBAL_CLUE"
              @init="setFilterConfigList"
            >
              <template #searchTableTotal="{ total }">
                <globalSearchResult :title="currentTitle" :total="total" @init-total="initTotal" />
              </template>
            </clueTable>
            <cluePoolTable
              v-else-if="activeTab === FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL"
              ref="cluePoolTableRef"
              readonly
              hidden-total
              hidden-pool-select
              hidden-advance-filter
              :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL"
              @init="setFilterConfigList"
            >
              <template #searchTableTotal="{ total }">
                <globalSearchResult :title="currentTitle" :total="total" @init-total="initTotal" />
              </template>
            </cluePoolTable>
          </Suspense>
        </CrmCard>
      </div>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { ConditionsItem, FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import globalSearchResult from './globalSearchResult.vue';
  import clueTable from '@/views/clueManagement/clue/components/clueTable.vue';
  import cluePoolTable from '@/views/clueManagement/cluePool/components/cluePoolTable.vue';
  import customerTable from '@/views/customer/components/customerTable.vue';
  import openSeaTable from '@/views/customer/components/openSeaTable.vue';
  import opportunityTable from '@/views/opportunity/components/opportunityTable.vue';

  import { lastScopedOptions } from '../config';

  const { t } = useI18n();

  const props = defineProps<{
    formKey?: FormDesignKeyEnum | null;
    keyword: string;
  }>();

  const emit = defineEmits<{
    (e: 'showCountDetail', row: Record<string, any>, type: 'opportunity' | 'clue'): void;
    (e: 'close'): void;
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
  const globalKeyword = ref('');

  const isAdvancedSearchMode = ref(false);
  const filterResult = ref<FilterResult>({ searchMode: 'AND', conditions: [] });

  const activeTab = ref(FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER);

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

  const opportunityTableRef = ref<InstanceType<typeof opportunityTable>>();
  const customerTableRef = ref<InstanceType<typeof customerTable>>();
  const contactTableRef = ref<InstanceType<typeof ContactTable>>();
  const openSeaTableRef = ref<InstanceType<typeof openSeaTable>>();
  const clueTableRef = ref<InstanceType<typeof clueTable>>();
  const cluePoolTableRef = ref<InstanceType<typeof cluePoolTable>>();

  function loadList(filter: FilterResult) {
    switch (activeTab.value) {
      case FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY:
        opportunityTableRef.value?.handleAdvanceFilter?.(filter, isAdvancedSearchMode.value, globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER:
        customerTableRef.value?.handleAdvanceFilter?.(filter, isAdvancedSearchMode.value, globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT:
        contactTableRef.value?.handleAdvanceFilter?.(filter, isAdvancedSearchMode.value, globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC:
        openSeaTableRef.value?.handleAdvanceFilter?.(filter, isAdvancedSearchMode.value, globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CLUE:
        clueTableRef.value?.handleAdvanceFilter?.(filter, isAdvancedSearchMode.value, globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL:
        cluePoolTableRef.value?.handleAdvanceFilter?.(filter, isAdvancedSearchMode.value, globalKeyword.value);
        break;
      default:
        break;
    }
  }

  function initSearchData() {
    switch (activeTab.value) {
      case FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY:
        opportunityTableRef.value?.handleSearchData?.(globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER:
        customerTableRef.value?.handleSearchData?.(globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT:
        contactTableRef.value?.handleSearchData?.(globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC:
        openSeaTableRef.value?.handleSearchData?.(globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CLUE:
        clueTableRef.value?.handleSearchData?.(globalKeyword.value);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL:
        cluePoolTableRef.value?.handleSearchData?.(globalKeyword.value);
        break;
      default:
        break;
    }
  }

  const handleFilterConditions = (filter: FilterResult) => {
    const haveConditions: boolean =
      filter.conditions?.some((item) => {
        const valueCanEmpty = ['EMPTY', 'NOT_EMPTY'].includes(item.operator as string);
        return valueCanEmpty || getIsValidValue(item);
      }) ?? false;

    isAdvancedSearchMode.value = haveConditions;
    filterResult.value = filter;
    loadList(filter);
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

  const configList = ref<FilterFormItem[]>([]);
  const customList = ref<FilterFormItem[]>([]);

  function setFilterConfigList(params: Record<string, any>) {
    const { filterConfigList, customFieldsFilterConfig } = params;
    configList.value = filterConfigList;
    customList.value = customFieldsFilterConfig;
    initSearchData();
  }

  watch(
    () => props.formKey,
    (val) => {
      globalKeyword.value = '';
      if (val) {
        globalKeyword.value = props.keyword;
        activeTab.value = val as FormDesignKeyEnum;
      }
    },
    {
      immediate: true,
    }
  );

  const tabList = computed(() => lastScopedOptions.value.map((e) => ({ name: e.value, tab: e.label })));

  const currentTitle = computed(() => lastScopedOptions.value.find((e) => e.value === activeTab.value)?.label ?? '');

  const totalCount = ref(0);
  function initTotal(val: number) {
    totalCount.value = val;
  }
  function handleCancel() {
    clearFilter();
    activeTab.value = lastScopedOptions.value[0]?.value;
    emit('close');
  }
</script>

<style scoped lang="less">
  .global-search-wrapper {
    padding: 16px;
    background: var(--text-n9);
    @apply h-full w-full;
    .global-search {
      @apply h-full  w-full overflow-y-auto;
      .crm-scroll-bar();
    }
  }
</style>
