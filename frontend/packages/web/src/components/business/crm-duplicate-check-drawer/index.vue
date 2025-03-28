<template>
  <CrmDrawer v-model:show="visible" :width="800" :footer="false" :title="t('workbench.duplicateCheck')">
    <CrmSearchInput
      v-model:value="keyword"
      class="mb-[24px] !w-full"
      :placeholder="t('workbench.duplicateCheck.inputPlaceholder')"
      @search="(val) => searchData(val)"
    />

    <!-- TODO lmy 没数据样式 -->
    <!-- 查询结果 -->
    <div class="mb-[24px]">
      <div class="font-semibold">{{ t('workbench.duplicateCheck.result') }}</div>
      <div class="mt-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
        <CrmTable
          v-bind="propsRes"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
        />
      </div>
    </div>
    <div class="mb-[24px]">
      <!-- TODO lmy 联调 -->
      <RelatedTable
        ref="clueTableRef"
        :api="GetRepeatCustomerData"
        :columns="clueColumns"
        :title="t('workbench.duplicateCheck.relatedClues')"
      />
    </div>
  </CrmDrawer>

  <CrmDrawer v-model:show="showDetailDrawer" :width="800" :footer="false" :title="activeCustomer?.name">
    <!-- TODO lmy 联调 -->
    <RelatedTable
      ref="detailTableRef"
      :api="detailType === 'opportunity' ? GetRepeatCustomerData : GetRepeatCustomerData"
      :columns="detailType === 'opportunity' ? opportunityColumns : clueColumns"
      :title="
        detailType === 'opportunity'
          ? t('workbench.duplicateCheck.relatedOpportunity')
          : t('workbench.duplicateCheck.relatedClue')
      "
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import RelatedTable from './components/relatedTable.vue';

  import { GetRepeatCustomerData } from '@/api/modules/system/business';
  import { useI18n } from '@/hooks/useI18n';

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();

  const keyword = ref('');

  const activeCustomer = ref();
  const showDetailDrawer = ref(false);
  const detailType = ref<'opportunity' | 'clue'>('clue');

  const customerClueTableRef = ref<InstanceType<typeof RelatedTable>>();
  function showDetail(row: any, type: 'opportunity' | 'clue') {
    activeCustomer.value = row;
    detailType.value = type;
    showDetailDrawer.value = true;
    customerClueTableRef.value?.searchData();
  }

  const clueColumns: CrmDataTableColumn[] = [
    {
      title: t('opportunity.customerName'),
      key: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('workbench.duplicateCheck.clueStage'),
      key: 'stage',
      width: 100,
    },
    {
      title: t('common.head'),
      key: 'owner',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  const opportunityColumns: CrmDataTableColumn[] = [
    {
      title: t('opportunity.name'),
      key: 'name',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('opportunity.customerName'),
      key: 'customerName',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('opportunity.intendedProducts'),
      key: 'intendedProducts',
      width: 150,
    },
    {
      title: t('opportunity.stage'),
      width: 150,
      key: 'stage',
    },
    {
      title: t('common.head'),
      key: 'owner',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

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
      key: 'status',
      width: 80,
    },
    {
      title: t('common.head'),
      key: 'stage',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('workbench.duplicateCheck.relatedOpportunity'),
      key: 'relatedOpportunity',
      width: 80,
      render: (row: any) =>
        h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => showDetail(row, 'opportunity'),
          },
          { default: () => row.relatedOpportunity }
        ),
    },
    {
      title: t('workbench.duplicateCheck.relatedClue'),
      key: 'relatedClues',
      width: 80,
      render: (row: any) =>
        h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => showDetail(row, 'clue'),
          },
          { default: () => row.relatedClues }
        ),
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
    },
  ];

  // TODO lmy 联调
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(GetRepeatCustomerData, {
    showSetting: false,
    columns,
  });

  const clueTableRef = ref<InstanceType<typeof RelatedTable>>();
  async function searchData(val: string) {
    setLoadListParams({ keyword: val });
    loadList();
    clueTableRef.value?.searchData(val);
  }
</script>
