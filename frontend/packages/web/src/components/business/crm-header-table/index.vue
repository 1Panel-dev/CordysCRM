<template>
  <CrmCard hide-footer>
    <CrmTable
      ref="crmTableRef"
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    >
      <template #tableTop>
        <div class="flex items-center justify-between">
          <div class="font-medium text-[var(--text-n1)]"> {{ t('opportunity.headRecordPage') }} </div>
          <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
        </div>
      </template>
    </CrmTable>
  </CrmCard>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import dayjs from 'dayjs';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CustomerContractTableParams, HeaderHistoryItem } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmTableDataItem } from '@/components/pure/crm-table/type';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  const { t } = useI18n();

  const props = defineProps<{
    sourceId: string; // 资源id
    loadListApi: (data: CustomerContractTableParams) => Promise<CrmTableDataItem<HeaderHistoryItem>>;
  }>();

  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.head'),
      key: 'owner',
      sortOrder: false,
      sorter: 'default',
      render: (row: HeaderHistoryItem) => {
        return h(CrmNameTooltip, { text: row.ownerName });
      },
      columnSelectorDisabled: true,
    },
    {
      title: t('opportunity.department'),
      key: 'departmentId',
      render: (row: HeaderHistoryItem) => {
        return h(CrmNameTooltip, { text: row.departmentName });
      },
    },
    {
      title: t('opportunity.belongStartTime'),
      key: 'collectionTime',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: 'default',
    },
    {
      title: t('opportunity.belongEndTime'),
      key: 'endTime',
      sortOrder: false,
      sorter: 'default',
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.operator'),
      key: 'operator',
      resizable: false,
      render: (row: HeaderHistoryItem) => {
        return h(CrmNameTooltip, { text: row.operatorName });
      },
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<HeaderHistoryItem>(
    props.loadListApi,
    {
      tableKey: TableKeyEnum.OPPORTUNITY_HEAD_LIST,
      showSetting: true,
      showPagination: false,
      columns,
    },
    (row: HeaderHistoryItem) => {
      return {
        ...row,
        collectionTime: dayjs(row.collectionTime).format('YYYY-MM-DD HH:mm:ss'),
        endTime: dayjs(row.endTime).format('YYYY-MM-DD HH:mm:ss'),
      };
    }
  );
  const keyword = ref('');

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  function initData() {
    setLoadListParams({
      sourceId: props.sourceId,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(val: string) {
    if (val.length) {
      const lowerCaseVal = val.toLowerCase();
      propsRes.value.data = propsRes.value.data.filter((item: HeaderHistoryItem) => {
        return item.ownerName?.toLowerCase().includes(lowerCaseVal);
      });
    } else {
      initData();
    }
  }

  watch(
    () => keyword.value,
    (val) => {
      if (!val.length) {
        initData();
      }
    }
  );

  onBeforeMount(() => {
    initData();
  });
</script>

<style lang="less" scoped></style>
