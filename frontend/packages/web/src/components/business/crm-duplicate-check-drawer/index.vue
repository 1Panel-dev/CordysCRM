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
        <n-button type="primary" @click="() => openGlobalSearch()">
          {{ t('workbench.duplicateCheck.searchInCordys') }}
        </n-button>
      </div>
      <!-- 查询结果 -->
      <template v-if="keyword.length">
        <template v-for="table in activeTables" :key="table.key">
          <div class="mb-[24px]">
            <div class="flex items-center font-semibold">
              {{ table.label }}
              <div class="text-[var(--text-n4)]"> ({{ table.instance.propsRes.value.crmPagination?.itemCount }}) </div>
            </div>
            <div class="mt-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
              <CrmTable
                v-bind="table.instance.propsRes.value"
                class="h-[205px]"
                @page-size-change="table.instance.propsEvent.value.pageSizeChange"
                @sorter-change="table.instance.propsEvent.value.sorterChange"
                @filter-change="table.instance.propsEvent.value.filterChange"
                @page-change="table.instance.propsEvent.value.pageChange"
              />

              <div v-show="table.instance.propsRes.value.crmPagination?.itemCount" class="flex justify-end">
                <n-button text type="primary" @click="openGlobalSearch(table.value)">
                  {{ t('common.ViewMore') }}
                </n-button>
              </div>
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
  <GlobalSearchDrawer v-model:visible="showGlobalSearchDrawer" :keyword="keyword" :form-key="globalSearchFormKey" />
</template>

<script setup lang="ts">
  import { NButton, NScrollbar } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { RepeatContactItem, RepeatCustomerItem } from '@lib/shared/models/system/business';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import { getFormListApiMap } from '@/components/business/crm-form-create/config';
  import GlobalSearchDrawer from './components/globalSearchDrawer.vue';
  import RelatedTable from './components/relatedTable.vue';

  import { getGlobalSearchClueDetail, globalSearchOptDetail } from '@/api/modules';
  // import { clueBaseSteps } from '@/config/clue';
  import { lastOpportunitySteps } from '@/config/opportunity';
  import { hasAnyPermission } from '@/utils/permission';

  import { lastScopedOptions } from './config';

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();

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

  const clueColumns: CrmDataTableColumn[] = [
    {
      title: t('workbench.duplicateCheck.company'),
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
              // TODO 跳转详情
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
    // TODO 区域
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
      title: t('workbench.duplicateCheck.company'),
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
              // TODO 跳转详情
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    // TODO 区域
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
              // TODO 跳转详情
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
    // TODO 区域
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
              // TODO 跳转详情
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
    // TODO 区域
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
              // TODO 跳转详情
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    // TODO 区域和类型
    {
      title: t('common.type'),
      key: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
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
              // TODO 跳转详情
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
    // TODO 部门
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
