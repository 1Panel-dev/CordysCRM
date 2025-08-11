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
      <CrmSearchInput
        v-model:value="keyword"
        class="mb-[24px] !w-full"
        auto-search
        :placeholder="t('workbench.duplicateCheck.inputPlaceholder')"
        @search="(val) => searchData(val)"
      />
      <!-- 查询结果 -->
      <template v-for="table in activeTables" :key="table.key">
        <div v-show="table.instance.propsRes.value.crmPagination?.itemCount" class="mb-[24px]">
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

            <div class="flex justify-end">
              <n-button text type="primary" @click="openGlobalSearch">
                {{ t('common.ViewMore') }}
              </n-button>
            </div>
          </div>
        </div>
      </template>
    </n-scrollbar>
  </CrmDrawer>

  <CrmDrawer v-model:show="showDetailDrawer" :width="800" :footer="false" :title="activeCustomer?.name">
    <RelatedTable
      ref="detailTableRef"
      :api="detailType === 'opportunity' ? GetRepeatOpportunityDetailList : GetRepeatClueDetailList"
      :columns="detailType === 'opportunity' ? opportunityColumns : clueColumns"
      :title="
        detailType === 'opportunity'
          ? t('workbench.duplicateCheck.relatedOpportunity')
          : t('workbench.duplicateCheck.relatedClue')
      "
      class="crm-detail-related-table"
    />
  </CrmDrawer>
  <GlobalSearchDrawer v-model:visible="showGlobalSearchDrawer" :form-key="globalSearchFormKey" />
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
  import { getFormListApiMap } from '@/components/business/crm-form-create/config';
  import GlobalSearchDrawer from './components/globalSearchDrawer.vue';
  import RelatedTable from './components/relatedTable.vue';

  import { GetRepeatClueDetailList, GetRepeatOpportunityDetailList } from '@/api/modules';
  // import { clueBaseSteps } from '@/config/clue';
  import { lastOpportunitySteps } from '@/config/opportunity';
  import { hasAnyPermission } from '@/utils/permission';

  import { scopedOptions } from './config';

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
    },
    // TODO 先不要了
    // {
    //   title: t('workbench.duplicateCheck.clueStage'),
    //   key: 'stage',
    //   width: 100,
    //   render: (row) => {
    //     const step = [...clueBaseSteps, ...opportunityResultSteps].find((e: any) => e.value === row.stage);
    //     return step ? step.label : '-';
    //   },
    // },
    {
      title: t('common.head'),
      key: 'ownerName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    // TODO 先不要了
    // {
    //   title: t('workbench.duplicateCheck.contactorName'),
    //   key: 'contact',
    //   width: 100,
    //   ellipsis: {
    //     tooltip: true,
    //   },
    // },
    // {
    //   title: t('workbench.duplicateCheck.contactorPhoneNumber'),
    //   key: 'phone',
    //   width: 100,
    //   ellipsis: {
    //     tooltip: true,
    //   },
    // },
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

  const opportunityColumns: CrmDataTableColumn[] = [
    {
      title: t('opportunity.name'),
      key: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
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

  const statusOption = [
    { label: t('workbench.duplicateCheck.duplicate'), value: 'ALL' },
    { label: t('workbench.duplicateCheck.similar'), value: 'PART' },
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
    },
    {
      title: t('workbench.duplicateCheck.status'),
      key: 'repeatType',
      width: 70,
      render: (row) => {
        const statusOptionItem = statusOption.find((e) => e.value === row.repeatType);
        return statusOptionItem ? statusOptionItem.label : '-';
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

  // 联系人相关
  const contactColumn: CrmDataTableColumn[] = [
    {
      title: t('opportunity.customerName'),
      key: 'customerName',
      width: 100,
      ellipsis: {
        tooltip: true,
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
    [FormDesignKeyEnum.SEARCH_GLOBAL_PUBLIC]: columns,
    [FormDesignKeyEnum.SEARCH_GLOBAL_CLUE]: clueColumns,
    [FormDesignKeyEnum.SEARCH_GLOBAL_CLUE_POOL]: columns,
    [FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY]: opportunityColumns,
  };

  const tables = scopedOptions.value.map((config) => ({
    ...config,
    instance: useTable(getFormListApiMap[config.value], {
      showSetting: false,
      columns: columnsMap[config.value],
      crmPagination: { size: 'small' },
      hiddenTotal: true,
      hiddenRefresh: true,
      hiddenAllScreen: true,
    }),
    enable: true, // TODO 模块是否开启
  }));

  const activeTables = computed(() => tables.filter((table) => table.enable));

  const searchData = (val: string) => {
    const searchTerm = val.replace(/[\s\uFEFF\xA0]+/g, '');
    activeTables.value.forEach(async (table) => {
      table.instance.setLoadListParams({ name: searchTerm });
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
  const globalSearchFormKey = ref(FormDesignKeyEnum.SEARCH_GLOBAL_OPPORTUNITY);
  function openGlobalSearch() {
    showGlobalSearchDrawer.value = true;
  }
</script>
