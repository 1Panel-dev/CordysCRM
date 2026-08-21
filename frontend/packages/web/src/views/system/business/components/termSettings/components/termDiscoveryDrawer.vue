<template>
  <CrmDrawer v-model:show="showDrawer" :title="t('system.business.term.aiDiscovery')" :width="960" :footer="false">
    <CrmTable
      ref="crmTableRef"
      v-bind="propsRes"
      class="term-discovery-table"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @refresh="searchData"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { h, ref, watch } from 'vue';
  import { useMessage } from 'naive-ui';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { TermDiscoveryItem } from '@lib/shared/models/system/term';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { getTermDiscoveryList, ignoreTermDiscovery } from '@/api/modules';

  const emit = defineEmits<{
    (e: 'adopt', row: TermDiscoveryItem): void;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const { t } = useI18n();
  const Message = useMessage();
  const tableRemoveRefreshId = ref('');

  async function handleIgnore(row: TermDiscoveryItem) {
    try {
      await ignoreTermDiscovery(row.id);
      Message.success(t('system.business.term.ignoreSuccess'));
      tableRemoveRefreshId.value = row.id;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleActionSelect(row: TermDiscoveryItem, actionKey: string) {
    switch (actionKey) {
      case 'adopt':
        emit('adopt', row);
        break;
      case 'ignore':
        handleIgnore(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn<TermDiscoveryItem>[] = [
    {
      title: t('system.business.term.undefinedTerm'),
      key: 'freeTerm',
      width: 160,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('system.business.term.discoverySource'),
      key: 'source',
      width: 160,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('system.business.term.discoveryTime'),
      key: 'createTime',
      width: 180,
    },
    {
      title: t('system.business.term.suggestedMapping'),
      key: 'reference',
      minWidth: 280,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 130,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: [
            { label: t('system.business.term.adopt'), key: 'adopt' },
            { label: t('system.business.term.ignore'), key: 'ignore' },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<TermDiscoveryItem>(getTermDiscoveryList, {
    columns,
    tableKey: TableKeyEnum.SYSTEM_TERM_DISCOVERY,
    showSetting: false,
    containerClass: '.term-discovery-table',
  });

  function removeItemFromList(id: string) {
    propsRes.value.data = propsRes.value.data.filter((item) => item.id !== id) as typeof propsRes.value.data;
    propsRes.value.crmPagination = {
      ...propsRes.value.crmPagination,
      itemCount: Math.max((propsRes.value.crmPagination?.itemCount ?? 1) - 1, 0),
    };
  }

  defineExpose({
    removeItemFromList,
  });

  watch(
    () => tableRemoveRefreshId.value,
    (val) => {
      if (val) {
        removeItemFromList(val);
      }
    }
  );

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  async function searchData() {
    try {
      setLoadListParams();
      await loadList();
      crmTableRef.value?.scrollTo({ top: 0 });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => showDrawer.value,
    (visible) => {
      if (visible) {
        searchData();
      }
    }
  );
</script>
