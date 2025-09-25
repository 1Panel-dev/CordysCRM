<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <CrmTable
      ref="crmTableRef"
      v-bind="propsRes"
      class="crm-header-table"
      :scroll-x="1000"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @refresh="initData"
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

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
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

  import useReasonConfig from '@/hooks/useReasonConfig';

  const { t } = useI18n();

  const props = defineProps<{
    formKey: FormDesignKeyEnum;
    sourceId: string; // 资源id
    loadListApi: (data: CustomerContractTableParams) => Promise<CrmTableDataItem<HeaderHistoryItem>>;
  }>();

  const { reasonEnable, initReasonConfig, reasonOptions } = useReasonConfig(props.formKey);

  await initReasonConfig();

  const columns = computed<CrmDataTableColumn[]>(() => [
    {
      title: t('common.head'),
      key: 'owner',
      width: 120,
      sortOrder: false,
      sorter: 'default',
      render: (row: HeaderHistoryItem) => {
        return h(CrmNameTooltip, { text: row.ownerName });
      },
      fixed: 'left',
      columnSelectorDisabled: true,
    },
    {
      title: t('opportunity.department'),
      key: 'departmentId',
      width: 120,
      render: (row: HeaderHistoryItem) => {
        return h(CrmNameTooltip, { text: row.departmentName });
      },
    },
    {
      title: t('opportunity.belongStartTime'),
      key: 'collectionTime',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: 'default',
    },
    {
      title: t('opportunity.belongEndTime'),
      key: 'endTime',
      width: 120,
      sortOrder: false,
      sorter: 'default',
      ellipsis: {
        tooltip: true,
      },
    },
    ...((reasonEnable.value
      ? [
          {
            title: t('customer.recycleReason'),
            width: 120,
            key: 'reasonId',
            ellipsis: {
              tooltip: true,
            },
            sortOrder: false,
            sorter: 'default',
            filter: 'default',
            filterOptions: reasonOptions.value,
            render: (row: any) => row.reasonName || '-',
          },
        ]
      : []) as CrmDataTableColumn[]),
    {
      title: t('common.operator'),
      key: 'operator',
      width: 100,
      resizable: false,
      render: (row: HeaderHistoryItem) => {
        return h(CrmNameTooltip, { text: row.operatorName });
      },
    },
  ]);

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<HeaderHistoryItem>(
    props.loadListApi,
    {
      tableKey: TableKeyEnum.OPPORTUNITY_HEAD_LIST,
      showSetting: true,
      showPagination: false,
      columns: columns.value,
      containerClass: '.crm-header-table',
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
