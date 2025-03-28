<template>
  <div class="font-semibold">{{ props.title }}</div>
  <div class="mt-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
  </div>
</template>

<script setup lang="ts">
  import type { CommonList } from '@lib/shared/models/common';

  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  const props = defineProps<{
    api: (data: any) => Promise<CommonList<any>>;
    columns: CrmDataTableColumn[];
    title: string;
  }>();

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(props.api, {
    showSetting: false,
    columns: props.columns,
  });

  async function searchData(val?: string) {
    setLoadListParams({ ...(val ? { keyword: val } : {}) });
    loadList();
  }

  defineExpose({
    searchData,
  });
</script>
