<template>
  <CrmCard hide-footer :special-height="64">
    <div class="mb-[16px] flex items-center justify-between">
      <div class="font-medium text-[var(--text-n1)]"> {{ t('opportunity.headRecordPage') }} </div>
      <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
    </div>
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
  </CrmCard>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { CommonList, TableQueryParams } from '@lib/shared/models/common';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    sourceId: string; // 资源id
    loadListApi: (params: TableQueryParams) => Promise<CommonList<any>>;
  }>();

  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.head'),
      key: 'name',
      width: 150,
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('opportunity.belongStatus'),
      key: 'belongStatus',
      width: 100,
      sortOrder: false,
      sorter: true,
      filterOptions: [
        {
          label: t('common.enable'),
          value: 1,
        },
        {
          label: t('common.disable'),
          value: 0,
        },
      ],
    },
    {
      title: t('opportunity.department'),
      key: 'departmentName',
      ellipsis: {
        tooltip: true,
      },
      width: 150,
    },
    {
      title: t('opportunity.region'),
      key: 'region',
      ellipsis: {
        tooltip: true,
      },
      width: 150,
    },
    {
      title: t('opportunity.belongStartTime'),
      key: 'belongStartTime',
      ellipsis: {
        tooltip: true,
      },
      width: 150,
    },
    {
      title: t('opportunity.belongEndTime'),
      key: 'belongEndTime',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      // TODO ts类型
      render: (row: any) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(props.loadListApi, {
    tableKey: TableKeyEnum.OPPORTUNITY_HEAD_LIST,
    showSetting: true,
    columns,
  });
  const keyword = ref('');

  function initData() {
    setLoadListParams({
      sourceId: props.sourceId,
      keyword: keyword.value,
    });
    loadList();
  }

  function searchData(val: string) {
    keyword.value = val;
    initData();
  }

  onBeforeMount(() => {
    initData();
  });
</script>

<style lang="less" scoped></style>
