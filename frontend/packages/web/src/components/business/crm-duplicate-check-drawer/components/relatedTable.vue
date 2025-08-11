<template>
  <div :class="props.class">
    <div class="flex items-center font-semibold">
      {{ props.title }}
      <div class="text-[var(--text-n4)]"> （{{ propsRes.crmPagination?.itemCount || 0 }}） </div>
    </div>
    <div class="mt-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
      <CrmTable
        ref="crmTableRef"
        v-bind="propsRes"
        class="!min-h-[548px]"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </div>
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
    class: string;
  }>();

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(props.api, {
    showSetting: false,
    columns: props.columns,
    crmPagination: {
      size: 'small',
    },
    hiddenRefresh: true,
    hiddenTotal: true,
    containerClass: props.class,
  });

  async function searchData(val: string, id?: string) {
    setLoadListParams({ name: val, ...(id ? { id } : {}) });
    await loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  defineExpose({
    searchData,
    propsRes,
  });
</script>
