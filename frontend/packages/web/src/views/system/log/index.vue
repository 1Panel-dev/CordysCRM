<template>
  <n-scrollbar x-scrollable :content-style="{ 'min-width': '1000px', 'width': '100%', 'height': '100%' }">
    <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
      <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
    </CrmCard>

    <CrmCard hide-footer auto-height class="form-card mb-[16px] min-w-[1000px]">
      <n-form ref="formRef" label-placement="left" inline label-width="auto" :model="form" class="flex-wrap">
        <n-form-item :label="t('common.operator')" path="operator">
          <CrmUserSelect
            v-model:value="form.operator"
            value-field="id"
            label-field="name"
            mode="remote"
            class="w-[305px]"
            :fetch-api="getUserOptions"
            filterable
            clearable
          />
        </n-form-item>
        <n-form-item :label="t('log.operationTime')" path="time">
          <n-date-picker v-model:value="form.time" type="datetimerange" :is-date-disabled="dataDisabled" />
        </n-form-item>
        <template v-if="activeTab === 'operation'">
          <n-form-item :label="t('log.operationType')" path="type">
            <n-select
              v-model:value="form.type"
              class="w-[305px]"
              :options="logTypeOption"
              :placeholder="t('common.pleaseSelect')"
              clearable
            />
          </n-form-item>
          <n-form-item :label="t('log.operationScope')" path="module">
            <n-cascader
              v-model:value="form.module"
              :options="moduleOptions"
              class="w-[305px]"
              check-strategy="all"
              :placeholder="t('common.pleaseSelect')"
              clearable
            />
          </n-form-item>
        </template>
        <n-form-item>
          <n-button ghost class="mr-[12px]" type="primary" @click="searchData">
            {{ t('advanceFilter.filter') }}
          </n-button>
          <n-button type="default" class="outline--secondary" @click="handleReset">
            {{ t('common.reset') }}
          </n-button>
        </n-form-item>
      </n-form>
    </CrmCard>

    <CrmCard v-if="activeTab === 'operation'" hide-footer :special-height="208">
      <CrmTable
        v-bind="propsRes"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </CrmCard>
    <LoginLog v-if="activeTab === 'login'" ref="loginLogRef" />
  </n-scrollbar>

  <CrmDrawer v-model:show="showDetailDrawer" :footer="false" :show-mask="false" :title="t('log.detail')" :width="680">
    <LogDetailItem :detail="activeLogDetail" />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { CascaderOption, NButton, NCascader, NDatePicker, NForm, NFormItem, NScrollbar, NSelect } from 'naive-ui';
  import dayjs from 'dayjs';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { OperationTypeEnum } from '@lib/shared/enums/systemEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getCityPath } from '@lib/shared/method';
  import type { OperationLogDetail, OperationLogItem, OperationLogParams } from '@lib/shared/models/system/log';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';
  import LogDetailItem from './components/logDetailItem.vue';
  import LoginLog from './components/loginLog.vue';

  import { getUserOptions } from '@/api/modules';
  import { operationLogDetail, operationLogList } from '@/api/modules/system/log';
  import { logTypeOption } from '@/config/system';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import usePathMap from '@/hooks/usePathMap';

  const { t } = useI18n();
  const { getModuleOptions, findLocalePath } = usePathMap();

  const activeTab = ref('operation');
  const tabList = [
    {
      name: 'operation',
      tab: t('log.operationLog'),
    },
    {
      name: 'login',
      tab: t('log.loginLog'),
    },
  ];

  // 查询条件
  function dataDisabled(ts: number) {
    const currentDate = new Date();
    const sixMonthsAgo = new Date();
    sixMonthsAgo.setMonth(currentDate.getMonth() - 6);
    const selectedDate = new Date(ts);
    // 只能查询过去半年的
    return selectedDate < sixMonthsAgo || selectedDate > currentDate;
  }

  const moduleOptions = ref<CascaderOption[]>([]);
  function initModuleOptions() {
    moduleOptions.value = getModuleOptions();
  }

  const defaultForm = {
    type: null,
    module: null,
    operator: null,
    time: [dayjs().subtract(1, 'M').valueOf(), dayjs().valueOf()],
  };
  const form = ref<OperationLogParams>({
    ...defaultForm,
  });

  // 详情
  const activeLogDetail = ref<OperationLogDetail>();
  const showDetailDrawer = ref(false);

  function cityFormat(val: string) {
    const address = val?.split('-');
    return address ? `${getCityPath(address[0])}${address[1] ? `-${address[1]}` : ''}` : '-';
  }

  const moduleFormKeyMap: Record<string, FormDesignKeyEnum> = {
    PRODUCT_MANAGEMENT: FormDesignKeyEnum.PRODUCT,
    CUSTOMER_INDEX: FormDesignKeyEnum.CUSTOMER,
    CLUE_MANAGEMENT_CLUE: FormDesignKeyEnum.CLUE,
    OPPORTUNITY: FormDesignKeyEnum.BUSINESS,
    FOLLOW_UP_RECORD: FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER,
    FOLLOW_UP_PLAN: FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER,
  };

  async function getLogDetail(id: string) {
    try {
      activeLogDetail.value = await operationLogDetail(id);
      const locationIds = ['workCity'];
      // 处理地址字段的值
      const matchedKey = Object.keys(moduleFormKeyMap).find((module) => activeLogDetail.value?.module === module);
      if (matchedKey) {
        const { fieldList, initFormConfig } = useFormCreateApi({
          formKey: ref(moduleFormKeyMap[matchedKey]),
        });
        await initFormConfig();
        const locationId = fieldList.value.find((item) => item.type === FieldTypeEnum.LOCATION)?.id;
        if (locationId) {
          locationIds.push(locationId);
        }
      }
      activeLogDetail.value.diffs?.forEach((item) => {
        if (locationIds.includes(item.column)) {
          item.newValueName = cityFormat(item.newValue);
          item.oldValueName = cityFormat(item.oldValue);
        }
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.operator'),
      key: 'operatorName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: OperationLogItem) =>
        h(
          CrmTableButton,
          {
            onClick: async () => {
              activeLogDetail.value = row;
              if (row.type === OperationTypeEnum.UPDATE) {
                await getLogDetail(row.id);
              }
              if (!showDetailDrawer.value) {
                showDetailDrawer.value = true;
              }
            },
          },
          { default: () => row.operatorName, trigger: () => row.operatorName }
        ),
    },
    {
      title: t('log.operationScope'),
      key: 'module',
      width: 100,
      render: (row) => {
        return findLocalePath(row.module)
          .map((e) => t(e) || row.module)
          .join('/');
      },
    },
    {
      title: t('log.operationType'),
      key: 'type',
      width: 100,
      render: (row) => {
        const step = logTypeOption.find((e) => e.value === row.type);
        return step ? step.label : '-';
      },
    },
    {
      title: t('role.operator'),
      key: 'resourceName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('log.operationTime'),
      key: 'createTime',
      width: 100,
      sortOrder: false,
      sorter: true,
      resizable: false,
    },
  ];
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(operationLogList, {
    showSetting: false,
    columns,
    tableKey: TableKeyEnum.LOG,
  });

  const loginLogRef = ref<InstanceType<typeof LoginLog>>();
  async function searchData() {
    const { time, ...otherForm } = form.value;
    if (activeTab.value === 'operation') {
      setLoadListParams({ ...otherForm, startTime: time[0], endTime: time[1] });
      await loadList();
    } else {
      nextTick(() => {
        loginLogRef.value?.searchData({ operator: otherForm.operator, startTime: time[0], endTime: time[1] });
      });
    }
  }

  function handleReset() {
    form.value = { ...defaultForm };
    searchData();
  }

  watch(
    () => activeTab.value,
    () => {
      searchData();
    },
    { immediate: true }
  );

  onMounted(() => {
    initModuleOptions();
  });
</script>

<style lang="less" scoped>
  .form-card {
    :deep(.n-card__content) {
      padding: 24px 24px 8px;
    }
  }
  :deep(.n-form-item-feedback-wrapper) {
    min-height: 16px;
  }
</style>
