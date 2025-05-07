<template>
  <CrmDrawer v-model:show="visible" :width="800" :footer="false" :title="t('workbench.duplicateCheck')">
    <CrmSearchInput
      v-model:value="keyword"
      class="mb-[24px] !w-full"
      auto-search
      :placeholder="t('workbench.duplicateCheck.inputPlaceholder')"
      @search="(val) => searchData(val)"
    />
    <div v-show="noDuplicateCustomers" class="text-center text-[var(--text-n4)]">
      {{ t('workbench.duplicateCheck.noDuplicateCustomers') }}
    </div>
    <!-- 查询结果 -->
    <div v-show="showResult" class="mb-[24px]">
      <div class="font-semibold">{{ t('workbench.duplicateCheck.result') }}</div>
      <div v-show="code === 101003" class="text-center text-[var(--text-n4)]">
        {{ t('workbench.duplicateCheck.moduleNotEnabled') }}
      </div>
      <div v-show="code !== 101003" class="mt-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
        <CrmTable
          v-bind="propsRes"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
        />
      </div>
    </div>
    <RelatedTable
      v-show="showClue"
      ref="clueTableRef"
      :api="GetRepeatClueList"
      :columns="clueColumns"
      :title="t('workbench.duplicateCheck.relatedClues')"
      is-return-native-response
    />
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
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { RepeatCustomerItem } from '@lib/shared/models/system/business';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import RelatedTable from './components/relatedTable.vue';

  import {
    GetRepeatClueDetailList,
    GetRepeatClueList,
    GetRepeatCustomerList,
    GetRepeatOpportunityDetailList,
  } from '@/api/modules';
  import { clueBaseSteps } from '@/config/clue';
  import { lastOpportunitySteps, opportunityResultSteps } from '@/config/opportunity';

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();

  const keyword = ref('');

  const noDuplicateCustomers = ref(false);
  const showResult = ref(false);
  const showClue = ref(false);

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
    {
      title: t('workbench.duplicateCheck.clueStage'),
      key: 'stage',
      width: 100,
      render: (row) => {
        const step = [...clueBaseSteps, ...opportunityResultSteps].find((e: any) => e.value === row.stage);
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
      key: 'productNames',
      width: 200,
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
      width: 150,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  const statusOption = [
    { label: t('workbench.duplicateCheck.duplicate'), value: 'ALL' },
    { label: t('workbench.duplicateCheck.similar'), value: 'PART' },
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
      width: 70,
      render: (row: RepeatCustomerItem) =>
        h(
          NButton,
          {
            text: true,
            type: 'primary',
            disabled: !row.opportunityCount || !row.opportunityModuleEnable,
            onClick: () => showDetail(row, 'opportunity'),
          },
          { default: () => row.opportunityCount }
        ),
    },
    {
      title: t('workbench.duplicateCheck.relatedClue'),
      key: 'clueCount',
      width: 70,
      render: (row: RepeatCustomerItem) =>
        h(
          NButton,
          {
            text: true,
            type: 'primary',
            disabled: !row.clueCount || !row.clueModuleEnable,
            onClick: () => showDetail(row, 'clue'),
          },
          { default: () => row.clueCount }
        ),
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams, code } = useTable(GetRepeatCustomerList, {
    showSetting: false,
    columns,
    isReturnNativeResponse: true,
    crmPagination: {
      size: 'small',
    },
  });

  const clueTableRef = ref<InstanceType<typeof RelatedTable>>();
  async function searchData(val: string) {
    setLoadListParams({ name: val });
    loadList().finally(() => {
      showResult.value = !!propsRes.value.data.length || code.value === 101003;
      noDuplicateCustomers.value = !showResult.value && !showClue.value;
    });
    clueTableRef.value?.searchData(val).finally(() => {
      showClue.value = !!clueTableRef.value?.propsRes.data.length || clueTableRef.value?.code === 101003;
      noDuplicateCustomers.value = !showResult.value && !showClue.value;
    });
  }

  watch(
    () => visible.value,
    (val) => {
      if (!val) {
        keyword.value = '';
        showResult.value = false;
        showClue.value = false;
        noDuplicateCustomers.value = false;
      }
    }
  );
</script>
