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
              :options="lastScopedOptions"
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
              :max-filter-field-number="5"
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
              v-if="form.scoped === FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY"
              ref="opportunityTableRef"
              readonly
              hidden-advance-filter
              :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY"
              @init="setFilterConfigList"
            />
            <customerTable
              v-else-if="form.scoped === FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER"
              ref="customerTableRef"
              readonly
              hidden-advance-filter
              :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER"
              @init="setFilterConfigList"
            />
            <ContactTable
              v-else-if="form.scoped === FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT"
              ref="contactTableRef"
              readonly
              hidden-advance-filter
              :form-key="FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT"
              @init="setFilterConfigList"
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
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { ConditionsItem, FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import customerTable from '@/views/customer/components/customerTable.vue';
  import opportunityTable from '@/views/opportunity/components/opportunityTable.vue';

  import { useAppStore } from '@/store';

  const { t } = useI18n();
  const appStore = useAppStore();

  const props = defineProps<{
    formKey?: FormDesignKeyEnum | null;
    keyword: string;
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

  const scopedOptions = [
    {
      label: t('crmFormDesign.customer'),
      value: FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER,
      moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
    },
    {
      label: t('crmFormDesign.contract'),
      value: FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT,
      moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
    },
    {
      label: t('module.openSea'),
      value: FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC,
      moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
    },
    {
      label: t('crmFormDesign.clue'),
      value: FormDesignKeyEnum.SEARCH_GLOBAL_CLUE,
      moduleKey: ModuleConfigEnum.CLUE_MANAGEMENT,
    },
    {
      label: t('module.cluePool'),
      value: FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL,
      moduleKey: ModuleConfigEnum.CLUE_MANAGEMENT,
    },
    {
      label: t('module.businessManagement'),
      value: FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY,
      moduleKey: ModuleConfigEnum.BUSINESS_MANAGEMENT,
    },
  ];

  const lastScopedOptions = computed(() =>
    scopedOptions.filter((e) => appStore.moduleConfigList.find((m) => m.moduleKey === e.moduleKey && m.enable))
  );

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

  function loadList(filter: FilterResult) {
    switch (form.value.scoped) {
      case FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY:
        opportunityTableRef.value?.handleAdvanceFilter?.(filter);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER:
        customerTableRef.value?.handleAdvanceFilter?.(filter);
        break;
      case FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT:
        contactTableRef.value?.handleAdvanceFilter?.(filter);
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
    console.log(222);

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
  }

  watch(
    () => props.formKey,
    (val) => {
      if (val) {
        form.value.scoped = val as FormDesignKeyEnum;
      }
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
