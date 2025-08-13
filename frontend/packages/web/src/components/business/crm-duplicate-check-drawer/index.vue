<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :default-width="1000"
    :footer="false"
    class="min-w-[1000px]"
    :title="t('common.search')"
  >
    <n-scrollbar content-class="p-[24px]">
      <div class="mb-[24px] flex items-center justify-between gap-[12px]">
        <CrmSearchInput
          v-model:value="keyword"
          class="!w-full"
          auto-search
          :placeholder="t('workbench.duplicateCheck.inputPlaceholder')"
          @search="(val) => searchData(val)"
        />
        <n-button v-if="lastScopedOptions.length > 0" type="primary" @click="() => openGlobalSearch()">
          {{ t('workbench.duplicateCheck.searchInCordys') }}
        </n-button>
      </div>
      <!-- 查询结果 -->
      <template v-if="keyword.length">
        <template v-for="table in activeTables" :key="table.key">
          <div class="mb-[24px]">
            <div class="flex justify-between">
              <div class="flex items-center font-semibold">
                {{ table.label }}
                <div class="text-[var(--text-n4)]">
                  ({{ table.instance.propsRes.value.crmPagination?.itemCount }})
                </div>
              </div>
              <n-button
                v-show="table.instance.propsRes.value.crmPagination?.itemCount"
                text
                type="primary"
                @click="openGlobalSearch(table.value)"
              >
                {{ t('common.ViewMore') }}
              </n-button>
            </div>
            <div class="mt-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
              <CrmTable
                v-bind="table.instance.propsRes.value"
                class="!h-[205px]"
                @page-size-change="table.instance.propsEvent.value.pageSizeChange"
                @sorter-change="table.instance.propsEvent.value.sorterChange"
                @filter-change="table.instance.propsEvent.value.filterChange"
                @page-change="table.instance.propsEvent.value.pageChange"
              />
            </div>
          </div>
        </template>
      </template>
    </n-scrollbar>
  </CrmDrawer>

  <CrmDrawer v-model:show="showDetailDrawer" :width="800" :footer="false" :title="activeCustomer?.name">
    <RelatedTable
      ref="detailTableRef"
      :api="detailType === 'opportunity' ? globalSearchOptDetail : getGlobalSearchClueDetail"
      :columns="detailType === 'opportunity' ? opportunityColumns : clueColumns"
      :title="
        detailType === 'opportunity'
          ? t('workbench.duplicateCheck.relatedOpportunity')
          : t('workbench.duplicateCheck.relatedClue')
      "
      class="crm-detail-related-table"
    />
  </CrmDrawer>
  <GlobalSearchDrawer
    v-model:visible="showGlobalSearchDrawer"
    :keyword="keyword"
    :form-key="globalSearchFormKey"
    @show-count-detail="(row:any,type:'opportunity' | 'clue')=>showDetail(row,type)"
  />
  <!-- 详情 -->
  <Suspense>
    <div>
      <customerOverviewDrawer
        v-model:show="showCustomerOverviewDrawer"
        :source-id="activeSourceId"
        :readonly="isCustomerReadonly"
        @saved="searchData(keyword)"
      />
      <openSeaOverviewDrawer
        v-model:show="showCustomerOpenseaOverviewDrawer"
        :source-id="activeSourceId"
        :readonly="isCustomerReadonly"
        :pool-id="poolId"
        :hidden-columns="hiddenColumns"
        @change="searchData(keyword)"
      />

      <optOverviewDrawer
        v-model:show="showOptOverviewDrawer"
        :detail="activeOpportunity"
        @refresh="searchData(keyword)"
        @open-customer-drawer="handleOpenCustomerDrawer($event, true)"
      />
      <ClueOverviewDrawer
        v-if="isInitOverviewDrawer"
        v-model:show="showClueOverviewDrawer"
        :detail="activeClue"
        @refresh="searchData(keyword)"
        @open-customer-drawer="handleOpenCustomerDrawer($event, true)"
        @convert-to-customer="() => handleConvertToCustomer(activeClue)"
      />
      <CluePoolOverviewDrawer
        v-model:show="showCluePoolOverviewDrawer"
        :pool-id="poolId"
        :detail="activeClue as CluePoolListItem"
        :hidden-columns="cluePoolHiddenColumns"
        @refresh="searchData(keyword)"
      />
      <convertToCustomerDrawer
        v-if="isInitConvertDrawer"
        v-model:show="showConvertToCustomerDrawer"
        :clue-id="otherFollowRecordSaveParams.clueId"
        @finish="searchData(keyword)"
        @new="handleNewCustomer"
      />
      <CrmFormCreateDrawer
        v-if="isInitFormCreateDrawer"
        v-model:visible="formCreateDrawerVisible"
        :form-key="formKey"
        :need-init-detail="false"
        :initial-source-name="activeRowName"
        :other-save-params="otherFollowRecordSaveParams"
        :link-form-info="linkFormInfo"
        @saved="handleFormSaved"
      />
    </div>
  </Suspense>
</template>

<script setup lang="ts">
  import { NButton, NScrollbar, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ClueListItem, CluePoolListItem } from '@lib/shared/models/clue';
  import type { OpportunityItem } from '@lib/shared/models/opportunity';
  import { RepeatContactItem, RepeatCustomerItem } from '@lib/shared/models/system/business';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import { getFormListApiMap } from '@/components/business/crm-form-create/config';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import GlobalSearchDrawer from './components/globalSearchDrawer.vue';
  import RelatedTable from './components/relatedTable.vue';

  import {
    getGlobalSearchClueDetail,
    getOpenSeaOptions,
    getPoolOptions,
    globalSearchOptDetail,
    reTransitionCustomer,
  } from '@/api/modules';
  // import { clueBaseSteps } from '@/config/clue';
  import { lastOpportunitySteps } from '@/config/opportunity';
  import { hasAnyPermission } from '@/utils/permission';

  import { lastScopedOptions } from './config';

  const customerOverviewDrawer = defineAsyncComponent(
    () => import('@/views/customer/components/customerOverviewDrawer.vue')
  );
  const openSeaOverviewDrawer = defineAsyncComponent(
    () => import('@/views/customer/components/openSeaOverviewDrawer.vue')
  );
  const ClueOverviewDrawer = defineAsyncComponent(
    () => import('@/views/clueManagement/clue/components/clueOverviewDrawer.vue')
  );
  const CluePoolOverviewDrawer = defineAsyncComponent(
    () => import('@/views/clueManagement/cluePool/components/cluePoolOverviewDrawer.vue')
  );
  const convertToCustomerDrawer = defineAsyncComponent(
    () => import('@/views/clueManagement/clue/components/convertToCustomerDrawer.vue')
  );
  const optOverviewDrawer = defineAsyncComponent(() => import('@/views/opportunity/components/optOverviewDrawer.vue'));

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const Message = useMessage();

  const keyword = ref('');

  const activeCustomer = ref();
  const showDetailDrawer = ref(false);
  const detailType = ref<'opportunity' | 'clue'>('clue');

  const detailTableRef = ref<InstanceType<typeof RelatedTable>>();
  function showDetail(row: RepeatCustomerItem, type: 'opportunity' | 'clue') {
    activeCustomer.value = row;
    detailType.value = type;
    showDetailDrawer.value = true;
    nextTick(() => {
      detailTableRef.value?.searchData(row.name, row.id);
    });
  }

  // 概览
  const showCustomerOverviewDrawer = ref(false);
  const showCustomerOpenseaOverviewDrawer = ref(false);
  const poolId = ref<string>('');
  const activeSourceId = ref<string>('');

  const isCustomerReadonly = ref(false);
  function handleOpenCustomerDrawer(
    params: { customerId: string; inCustomerPool: boolean; poolId: string },
    readonly = false
  ) {
    activeSourceId.value = params.customerId;
    if (params.inCustomerPool) {
      if (hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ'])) {
        showCustomerOpenseaOverviewDrawer.value = true;
        poolId.value = params.poolId;
      } else {
        Message.warning(t('opportunity.noOpenSeaPermission'));
      }
    } else {
      showCustomerOverviewDrawer.value = true;
    }
    isCustomerReadonly.value = readonly;
  }

  const openSeaOptions = ref<CluePoolItem[]>([]);
  async function initOpenSeaOptions() {
    if (hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ'])) {
      const res = await getOpenSeaOptions();
      openSeaOptions.value = res;
    }
  }
  const hiddenColumns = computed<string[]>(() => {
    const openSeaSetting = openSeaOptions.value.find((item) => item.id === poolId.value);
    return openSeaSetting?.fieldConfigs.filter((item) => !item.enable).map((item) => item.fieldId) || [];
  });

  const showOptOverviewDrawer = ref<boolean>(false);
  const activeOpportunity = ref<OpportunityItem>();

  const isInitOverviewDrawer = ref(false);
  const showClueOverviewDrawer = ref(false);
  const showCluePoolOverviewDrawer = ref(false);
  const activeClue = ref<ClueListItem | CluePoolListItem>();

  const cluePoolOptions = ref<CluePoolItem[]>([]);
  async function getCluePoolOptions() {
    try {
      cluePoolOptions.value = await getPoolOptions();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }
  const cluePoolHiddenColumns = computed<string[]>(() => {
    const cluePoolSetting = cluePoolOptions.value.find((item) => item.id === poolId.value);
    return cluePoolSetting?.fieldConfigs.filter((item) => !item.enable).map((item) => item.fieldId) || [];
  });

  const isInitConvertDrawer = ref(false);
  const showConvertToCustomerDrawer = ref(false);
  const otherFollowRecordSaveParams = ref({
    type: 'CLUE',
    clueId: '',
    id: '',
  });
  const formKey = ref(FormDesignKeyEnum.CLUE);
  const activeRowName = ref('');
  function handleConvertToCustomer(row?: ClueListItem) {
    isInitConvertDrawer.value = true;
    formKey.value = FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER;
    activeRowName.value = row?.name || '';
    otherFollowRecordSaveParams.value.clueId = row?.id || '';
    showConvertToCustomerDrawer.value = true;
  }

  const formCreateDrawerVisible = ref(false);
  const isInitFormCreateDrawer = ref(false);
  const linkFormInfo = ref<Record<string, any> | undefined>({});
  function handleNewCustomer(formInfo: Record<string, any>) {
    isInitFormCreateDrawer.value = true;
    linkFormInfo.value = formInfo;
    formKey.value = FormDesignKeyEnum.CUSTOMER;
    formCreateDrawerVisible.value = true;
  }

  async function handleFormSaved(res: any) {
    if (linkFormInfo.value) {
      await reTransitionCustomer({
        clueId: otherFollowRecordSaveParams.value.clueId,
        customerId: res.id,
      });
    }
  }

  onBeforeMount(() => {
    initOpenSeaOptions();
    getCluePoolOptions();
  });

  const clueColumns: CrmDataTableColumn[] = [
    {
      title: t('crmFollowRecord.companyName'),
      key: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        if (!row.hasPermission) return row.name;
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeClue.value = row;
              isInitOverviewDrawer.value = true;
              showClueOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    {
      title: t('common.head'),
      key: 'ownerName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('org.department'),
      width: 120,
      key: 'departmentId',
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => row.departmentName || '-',
    },
    {
      title: t('opportunity.intendedProducts'),
      key: 'productNameList',
      width: 100,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
  ];

  const cluePoolColumns: CrmDataTableColumn[] = [
    {
      title: t('crmFollowRecord.companyName'),
      key: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        if (!row.hasPermission) return row.name;
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeClue.value = row;
              showCluePoolOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    {
      title: t('org.department'),
      width: 120,
      key: 'departmentId',
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => row.departmentName || '-',
    },
    {
      title: t('opportunity.intendedProducts'),
      key: 'productNameList',
      width: 100,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('common.creator'),
      key: 'createUserName',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  const opportunityColumns: CrmDataTableColumn[] = [
    {
      title: t('opportunity.name'),
      key: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        if (!row.hasPermission) return row.name;
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.id;
              activeOpportunity.value = row;
              showOptOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    {
      title: t('opportunity.customerName'),
      key: 'customerName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('org.department'),
      width: 120,
      key: 'departmentId',
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => row.departmentName || '-',
    },
    {
      title: t('opportunity.intendedProducts'),
      key: 'productNames',
      width: 100,
      isTag: true,
    },
    {
      title: t('opportunity.stage'),
      width: 100,
      key: 'stage',
      render: (row) => {
        const step = lastOpportunitySteps.find((e: any) => e.value === row.stage);
        return step ? step.label : '-';
      },
    },
    {
      title: t('common.head'),
      key: 'ownerName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  // 客户相关
  const columns: CrmDataTableColumn[] = [
    {
      title: t('opportunity.customerName'),
      key: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        if (!row.hasPermission) return row.name;
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.id;
              showCustomerOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    {
      title: t('common.head'),
      key: 'ownerName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('org.department'),
      width: 120,
      key: 'departmentId',
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => row.departmentName || '-',
    },
    {
      title: t('workbench.duplicateCheck.relatedOpportunity'),
      key: 'opportunityCount',
      width: 60,
      render: (row: RepeatCustomerItem) => {
        return !row.opportunityCount
          ? row.opportunityCount
          : h(
              NButton,
              {
                text: true,
                type: 'primary',
                disabled: !row.opportunityModuleEnable || !hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ']),
                onClick: () => showDetail(row, 'opportunity'),
              },
              { default: () => row.opportunityCount }
            );
      },
    },
    {
      title: t('workbench.duplicateCheck.relatedClue'),
      key: 'clueCount',
      width: 60,
      render: (row: RepeatCustomerItem) => {
        return !row.clueCount
          ? row.clueCount
          : h(
              NButton,
              {
                text: true,
                type: 'primary',
                disabled:
                  !row.clueModuleEnable || !hasAnyPermission(['CLUE_MANAGEMENT:READ', 'CLUE_MANAGEMENT_POOL:READ']),
                onClick: () => showDetail(row, 'clue'),
              },
              { default: () => row.clueCount }
            );
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  // 公海
  const openSeaColumns: CrmDataTableColumn[] = [
    {
      title: t('opportunity.customerName'),
      key: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        if (!row.hasPermission) return row.name;
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.id;
              poolId.value = row.poolId;
              showCustomerOpenseaOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    {
      title: t('org.department'),
      width: 120,
      key: 'departmentId',
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => row.departmentName || '-',
    },
    {
      title: t('common.creator'),
      key: 'createUserName',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  // 联系人相关
  const contactColumn: CrmDataTableColumn[] = [
    {
      title: t('opportunity.customerName'),
      key: 'customerName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        if (!row.hasPermission) return row.customerName;
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.customerId;
              showCustomerOverviewDrawer.value = true;
            },
          },
          { default: () => row.customerName, trigger: () => row.customerName }
        );
      },
    },
    {
      title: t('workbench.duplicateCheck.contactName'),
      key: 'name',
      width: 70,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.phoneNumber'),
      key: 'phone',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.head'),
      key: 'ownerName',
      width: 80,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('org.department'),
      width: 120,
      key: 'departmentId',
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => row.departmentName || '-',
    },
    {
      title: t('common.status'),
      width: 50,
      key: 'enable',
      ellipsis: {
        tooltip: true,
      },
      render: (row: RepeatContactItem) => {
        return row.enable ? t('common.open') : t('common.close');
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  const columnsMap: Partial<Record<FormDesignKeyEnum, CrmDataTableColumn[]>> = {
    [FormDesignKeyEnum.SEARCH_GLOBAL_CUSTOMER]: columns,
    [FormDesignKeyEnum.SEARCH_GLOBAL_CONTACT]: contactColumn,
    [FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC]: openSeaColumns,
    [FormDesignKeyEnum.SEARCH_GLOBAL_CLUE]: clueColumns,
    [FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL]: cluePoolColumns,
    [FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY]: opportunityColumns,
  };

  const activeTables = computed(() => {
    return lastScopedOptions.value.map((config) => ({
      ...config,
      instance: useTable(getFormListApiMap[config.value], {
        showSetting: false,
        columns: columnsMap[config.value],
        crmPagination: { size: 'small' },
        hiddenTotal: true,
        hiddenRefresh: true,
        hiddenAllScreen: true,
      }),
    }));
  });

  const searchData = (val: string) => {
    activeTables.value.forEach(async (table) => {
      table.instance.setLoadListParams({ keyword: val });
      await table.instance.loadList();
      if (table.instance.propsRes.value.data) {
        table.instance.propsRes.value.data = table.instance.propsRes.value.data.slice(0, 3);
      }
    });
  };

  watch(
    () => visible.value,
    (val) => {
      if (!val) {
        keyword.value = '';
      }
    }
  );

  const showGlobalSearchDrawer = ref(false);
  const globalSearchFormKey = ref();
  function openGlobalSearch(value?: FormDesignKeyEnum) {
    globalSearchFormKey.value = value;
    showGlobalSearchDrawer.value = true;
  }
</script>
