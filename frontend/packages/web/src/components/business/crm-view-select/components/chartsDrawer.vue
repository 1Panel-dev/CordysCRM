<template>
  <CrmDrawer v-model:show="show" :width="1000" :min-width="600" :footer="false">
    <template #titleLeft>
      <div class="flex items-center gap-[16px]">
        <div class="text-[16px] font-semibold">{{ t('crmViewSelect.dataAnalysis') }}</div>
        <n-select
          v-model:value="activeView"
          :options="options"
          filterable
          label-field="name"
          value-field="id"
          :reset-menu-on-options-change="false"
          :virtual-scroll="false"
          :show-checkmark="false"
          class="view-select w-[200px]"
          :style="{ '--crm-view-label': `'${t('crmViewSelect.view')}'` }"
        >
        </n-select>
      </div>
    </template>
    <div class="flex h-full flex-col">
      <n-collapse
        v-model:expanded-names="expandNames"
        :default-expanded-names="['1']"
        arrow-placement="right"
        display-directive="show"
      >
        <n-collapse-item name="1">
          <template #header>
            <div class="w-full text-[16px] font-semibold">
              {{ t('crmViewSelect.conditionFilter') }}
            </div>
          </template>
          <n-scrollbar class="max-h-[350px]">
            <FilterContent
              ref="filterContentRef"
              v-model:form-model="formModel"
              no-filter-option
              :config-list="props.configList"
              :custom-list="props.customList"
            />
          </n-scrollbar>
        </n-collapse-item>
      </n-collapse>
      <div class="mb-[16px] mt-[24px] text-[16px] font-semibold">{{ t('crmViewSelect.chartSetting') }}</div>
      <div class="mb-[24px] flex flex-wrap items-center gap-[24px]">
        <div class="filter-input">
          <div class="filter-input-label">{{ t('crmViewSelect.chartType') }}</div>
          <n-select v-model:value="chartType" :options="chartTypeOptions" filterable class="w-[120px]" />
        </div>
        <div class="filter-input">
          <div class="filter-input-label">{{ t('crmViewSelect.groupBy') }}</div>
          <n-select v-model:value="groupBy" :options="groupByOptions" filterable class="w-[195px]" />
        </div>
        <div class="filter-input">
          <div class="filter-input-label">{{ t('crmViewSelect.dataIndicator') }}</div>
          <n-input-group>
            <n-select v-model:value="dataIndicator" :options="dataIndicatorOptions" filterable class="w-[195px]" />
            <n-select
              v-model:value="aggregationMethod"
              :options="aggregationMethodOptions"
              :disabled="dataIndicator === 'COUNT'"
              :render-label="dataIndicatorMethodRenderLabel"
              class="w-[80px]"
            />
          </n-input-group>
        </div>
        <div class="filter-input flex-1">
          <n-tooltip trigger="hover" :disabled="loading || !!dataIndicator">
            <template #trigger>
              <n-button type="primary" ghost :disabled="loading || !dataIndicator" @click="generateChart">
                {{ t('crmViewSelect.generateChart') }}
              </n-button>
            </template>
            {{ t('crmViewSelect.noDataIndicator') }}
          </n-tooltip>
        </div>
      </div>
      <div
        class="min-h-[300px] flex-1 overflow-hidden rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]"
      >
        <div ref="chartContainerRef" class="h-full bg-[var(--text-n10)]">
          <n-spin :show="loading" class="h-full">
            <CrmChart
              v-if="seriesData.length > 0"
              :type="generatedChartType"
              :group-name="groupByName"
              :data-indicator-name="dataIndicatorName"
              :aggregation-method-name="aggregationMethodName"
              :x-data="xData"
              :data="seriesData"
              :container-ref="chartContainerRef"
              @chart-click="handleChartClick"
              @refresh="generateChart"
            />
            <n-empty v-else :show-icon="false">
              <div class="flex h-[300px] items-center">{{ t('common.noData') }}</div>
            </n-empty>
          </n-spin>
        </div>
      </div>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import {
    NButton,
    NCollapse,
    NCollapseItem,
    NEmpty,
    NInputGroup,
    NScrollbar,
    NSelect,
    NSpin,
    NTooltip,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';
  import { ViewItem } from '@lib/shared/models/view';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { ConditionsItem, FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import { getOperator, valueIsArray } from '@/components/pure/crm-advance-filter/utils';
  import CrmChart from '@/components/pure/crm-chart/index.vue';
  import { ChartTypeEnum } from '@/components/pure/crm-chart/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';

  import {
    generateContractChart,
    generateCustomerChart,
    generateCustomerContactChart,
    generateCustomerPoolChart,
    generateLeadChart,
    generateLeadPoolChart,
    generateOpportunityChart,
    generatePaymentPlanChart,
  } from '@/api/modules';
  import { TabType } from '@/hooks/useHiddenTab';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import useViewChartParams from '@/hooks/useViewChartParams';
  import useViewStore from '@/store/modules/view';

  import { InternalRowData } from 'naive-ui/es/data-table/src/interface';

  const props = defineProps<{
    type: TabType;
    configList: FilterFormItem[];
    customList?: FilterFormItem[];
    defaultViewId?: string;
    poolId?: string | number;
    advancedOriginalForm?: FilterForm;
    routeName?: string;
  }>();

  const emit = defineEmits<{
    (e: 'generatedChart', filterResult: FilterResult, filterForm: FilterForm, viewId: string): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openNewPage } = useOpenNewPage();
  const { setViewChartParams } = useViewChartParams();

  const show = defineModel<boolean>('show', {
    required: true,
  });
  const viewStore = useViewStore();
  const activeView = ref<string>(props.defaultViewId || '');
  const options = computed(() => [
    {
      type: 'group',
      name: t('crmViewSelect.systemView'),
      key: 'internal',
      children: [...(viewStore.internalViews as ViewItem[])].filter((item) => item.enable),
    },
    {
      type: 'group',
      name: t('crmViewSelect.myView'),
      key: 'custom',
      children: [...(viewStore.customViews as ViewItem[])].filter((item) => item.enable),
    },
  ]);

  const defaultFormModel: FilterForm = {
    searchMode: 'AND',
    list: [],
  };
  const formModel = ref<FilterForm>(cloneDeep(defaultFormModel));
  const filterContentRef = ref<InstanceType<typeof FilterContent>>();
  const chartType = ref<ChartTypeEnum>(ChartTypeEnum.BAR);
  const chartTypeOptions = [
    { label: t('crmViewSelect.bar'), value: ChartTypeEnum.BAR },
    { label: t('crmViewSelect.line'), value: ChartTypeEnum.LINE },
    { label: t('crmViewSelect.pie'), value: ChartTypeEnum.PIE },
    { label: t('crmViewSelect.doughnut'), value: ChartTypeEnum.DONUT },
    { label: t('crmViewSelect.funnel'), value: ChartTypeEnum.FUNNEL },
  ];
  const groupByOptions = computed(() =>
    [
      ...(props.customList || []),
      ...(props.configList || []).filter((e) => e.dataIndex === 'departmentId' || e.dataIndex === 'stage'),
    ]
      .filter(
        (e) =>
          ![
            FieldTypeEnum.TEXTAREA,
            FieldTypeEnum.PICTURE,
            FieldTypeEnum.ATTACHMENT,
            FieldTypeEnum.INPUT_MULTIPLE,
            FieldTypeEnum.LINK,
            FieldTypeEnum.SERIAL_NUMBER,
            FieldTypeEnum.INPUT,
            FieldTypeEnum.DATE_TIME,
          ].includes(e.type)
      )
      .map((item) => ({
        label: item.title,
        value: item.dataIndex || '',
      }))
  );
  const groupBy = ref<string>(groupByOptions.value[0]?.value || '');
  const groupByName = computed(() => groupByOptions.value.find((e) => e.value === groupBy.value)?.label || '');
  const dataIndicatorOptions = computed(() =>
    [...(props.customList || [])]
      .filter((e) => e.type === FieldTypeEnum.INPUT_NUMBER)
      .map((item) => ({
        label: item.title,
        value: item.dataIndex || '',
      }))
      .concat([{ label: t('crmViewSelect.counts'), value: 'COUNT' }])
  );
  const dataIndicator = ref<string>(dataIndicatorOptions.value[0]?.value || '');
  const dataIndicatorName = computed(
    () => dataIndicatorOptions.value.find((e) => e.value === dataIndicator.value)?.label || ''
  );
  const aggregationMethod = ref<'SUM' | 'AVG'>('SUM');
  const aggregationMethodOptions = [
    { label: t('crmViewSelect.sum'), value: 'SUM' },
    { label: t('crmViewSelect.average'), value: 'AVG' },
  ];
  const aggregationMethodName = computed(
    () => aggregationMethodOptions.find((e) => e.value === aggregationMethod.value)?.label || ''
  );

  function dataIndicatorMethodRenderLabel(option: { label: string; value: string }) {
    return dataIndicator.value === 'COUNT' ? t('crmViewSelect.count') : option.label;
  }

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

  const xData = ref<string[]>([]);
  const seriesData = ref<any[]>([]);
  const generateChartApiMap = {
    [FormDesignKeyEnum.CUSTOMER]: generateCustomerChart,
    [FormDesignKeyEnum.BUSINESS]: generateOpportunityChart,
    [FormDesignKeyEnum.CLUE]: generateLeadChart,
    [FormDesignKeyEnum.CLUE_POOL]: generateLeadPoolChart,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: generateCustomerPoolChart,
    [FormDesignKeyEnum.CONTACT]: generateCustomerContactChart,
    [FormDesignKeyEnum.FOLLOW_PLAN]: () => Promise.resolve([]),
    [FormDesignKeyEnum.FOLLOW_RECORD]: () => Promise.resolve([]),
    [FormDesignKeyEnum.CONTRACT]: generateContractChart,
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: generatePaymentPlanChart,
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: () => Promise.resolve([]),
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: () => Promise.resolve([]),
  };
  const loading = ref<boolean>(false);
  const expandNames = ref<string[]>(['1']);
  let chartCategoryMap: Record<string, any> = {};
  const generatedChartType = ref<ChartTypeEnum>(chartType.value);

  async function generateChart() {
    filterContentRef.value?.formRef?.validate(async (errors) => {
      if (errors) {
        return;
      }
      try {
        loading.value = true;
        const filterResult = getParams();
        const res = await generateChartApiMap[props.type]({
          filterCondition: filterResult,
          poolId: props.poolId,
          viewId: activeView.value,
          chartConfig: {
            chatType: chartType.value,
            categoryAxis: {
              fieldId: groupBy.value,
            },
            valueAxis: {
              fieldId: dataIndicator.value,
              aggregateMethod: dataIndicator.value === 'COUNT' ? 'COUNT' : aggregationMethod.value,
            },
          },
        });
        chartCategoryMap = {};
        xData.value = [];
        seriesData.value = [];
        generatedChartType.value = chartType.value;
        res.forEach((item) => {
          chartCategoryMap[item.categoryAxisName || t('crmFormDesign.none')] = item.categoryAxis;
          if ([ChartTypeEnum.LINE, ChartTypeEnum.BAR].includes(chartType.value)) {
            xData.value.push(item.categoryAxisName || t('crmFormDesign.none'));
          }
          seriesData.value.push({
            name: item.categoryAxisName || t('crmFormDesign.none'),
            value: item.valueAxis,
          });
        });
        emit('generatedChart', filterResult, formModel.value, activeView.value);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
        if (expandNames.value.length === 0) {
          expandNames.value = ['1'];
        }
      } finally {
        loading.value = false;
      }
    });
  }

  function handleChartClick(params: any) {
    if (formModel.value.searchMode === 'OR') {
      Message.warning(t('crmViewSelect.orConditionNotAllowJump'));
      return;
    }
    const systemFields = ['departmentId', 'stage'];
    const groupByField = computed(() => {
      const allFields = [
        ...(props.customList || []),
        ...(props.configList || []).filter((e) => systemFields.includes(e.dataIndex as string)),
      ];
      return allFields.find((e) => e.dataIndex === groupBy.value);
    });
    if (groupByField.value) {
      const chartKey = Date.now().toString();
      const form = {
        searchMode: formModel.value.searchMode,
        list: [...formModel.value.list],
      };
      const value = chartCategoryMap[params.name];
      let selectedRows: InternalRowData[] = [];
      let selectedUserList: SelectedUsersItem[] = [];
      let dataSourceProps = {};
      if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(groupByField.value.type)) {
        dataSourceProps = {
          dataSourceType: groupByField.value.dataSourceProps?.dataSourceType,
        };
        selectedRows = value ? [{ id: value, name: params.name }] : [];
      } else if (
        [
          FieldTypeEnum.DEPARTMENT,
          FieldTypeEnum.DEPARTMENT_MULTIPLE,
          FieldTypeEnum.MEMBER,
          FieldTypeEnum.MEMBER_MULTIPLE,
        ].includes(groupByField.value.type)
      ) {
        selectedUserList = value ? [{ id: value, name: params.name }] : [];
      }
      const tempField = {
        dataIndex: groupBy.value,
        operator: getOperator(groupByField.value.type, value),
        value,
        selectedRows,
        selectedUserList,
        dataSourceProps,
        type: groupByField.value.type || FieldTypeEnum.INPUT,
      };

      let selectedVal;
      if (valueIsArray(tempField)) {
        selectedVal = value ? [value] : [];
      } else {
        selectedVal = value;
      }

      form.list.push({
        ...tempField,
        value: selectedVal,
      });
      setViewChartParams(chartKey, {
        viewId: activeView.value,
        formModel: form as FilterForm,
      });
      openNewPage(props.routeName, {
        chartKey,
      });
    }
  }

  const chartContainerRef = ref<Element | undefined>(undefined);

  watch(
    () => show.value,
    (newVal) => {
      if (newVal) {
        formModel.value = cloneDeep(props.advancedOriginalForm || defaultFormModel);
        activeView.value = props.defaultViewId || '';
        setTimeout(() => {
          // 自动加载图表
          generateChart();
        });
      } else {
        seriesData.value = [];
        xData.value = [];
        groupBy.value = groupByOptions.value[0]?.value || '';
        dataIndicator.value = dataIndicatorOptions.value[0]?.value || '';
        chartType.value = ChartTypeEnum.BAR;
        aggregationMethod.value = 'SUM';
      }
    },
    { immediate: true }
  );
</script>

<style lang="less" scoped>
  .n-drawer-container {
    .view-select {
      font-weight: 400;
      :deep(.n-base-selection-label) {
        &::before {
          margin-left: 8px;
          white-space: nowrap;
          color: var(--text-n4);
          content: var(--crm-view-label);
        }
        .n-base-selection-label__render-label {
          padding-left: 40px;
        }
        .n-base-selection-placeholder {
          padding-left: 40px !important;
        }
        .n-base-selection-input {
          padding-left: 4px;
        }
      }
    }
    .filter-input {
      @apply flex items-center;

      gap: 16px;
      .filter-input-label {
        @apply break-keep;
      }
    }
  }
</style>
