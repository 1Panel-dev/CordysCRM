<template>
  <div>
    <div class="font-semibold">{{ props.title }}</div>
    <div v-show="code === 101003" class="text-center text-[var(--text-n4)]">
      {{ t('workbench.duplicateCheck.moduleNotEnabled') }}
    </div>
    <div v-show="code !== 101003" class="mt-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
      <CrmTable
        v-bind="propsRes"
        class="!h-[400px]"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CommonList } from '@lib/shared/models/common';

  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  const props = defineProps<{
    api: (data: any) => Promise<CommonList<any>>;
    columns: CrmDataTableColumn[];
    title: string;
    isReturnNativeResponse?: boolean;
  }>();

  const { t } = useI18n();

  const { propsRes, propsEvent, loadList, setLoadListParams, code } = useTable(props.api, {
    showSetting: false,
    columns: props.columns,
    crmPagination: {
      size: 'small',
    },
    isReturnNativeResponse: props.isReturnNativeResponse,
  });

  async function searchData(val: string, id?: string) {
    setLoadListParams({ name: val, ...(id ? { id } : {}) });
    await loadList();
  }

  defineExpose({
    searchData,
    propsRes,
    code,
  });
</script>
