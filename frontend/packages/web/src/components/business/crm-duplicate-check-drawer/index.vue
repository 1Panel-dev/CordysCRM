<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :default-width="1000"
    :footer="false"
    class="min-w-[1000px]"
    :title="t('workbench.duplicateCheck')"
  >
    <n-scrollbar content-class="p-[24px]">
      <CrmSearchInput
        v-model:value="keyword"
        class="mb-[24px] !w-full"
        auto-search
        :placeholder="t('workbench.duplicateCheck.inputPlaceholder')"
        @search="(val) => searchData(val)"
      />
      <div v-show="noDuplicateCustomers" class="text-center text-[var(--text-n4)]">
        {{
          validatePhone(keywordVal)
            ? t('workbench.duplicateCheck.noDuplicateContacts')
            : t('workbench.duplicateCheck.noDuplicateCustomers')
        }}
      </div>
      <!-- 查询结果 -->
      <div v-show="showResult" class="mb-[24px]">
        <div class="flex items-center font-semibold">
          {{
            validatePhone(keywordVal)
              ? t('workbench.duplicateCheck.contactResult')
              : t('workbench.duplicateCheck.result')
          }}
          <div class="text-[var(--text-n4)]"> （{{ repeatTable.propsRes.crmPagination.itemCount || 0 }}） </div>
        </div>
        <div v-show="repeatTable.code === 101003" class="text-center text-[var(--text-n4)]">
          {{ t('workbench.duplicateCheck.moduleNotEnabled') }}
        </div>
        <div
          v-show="repeatTable.code !== 101003"
          class="crm-repeat-table mt-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]"
        >
          <CrmTable
            ref="crmTableRef"
            v-bind="repeatTable.propsRes"
            class="!min-h-[548px]"
            @page-change="repeatTable.propsEvent.pageChange"
            @page-size-change="repeatTable.propsEvent.pageSizeChange"
            @sorter-change="repeatTable.propsEvent.sorterChange"
            @filter-change="repeatTable.propsEvent.filterChange"
          />
        </div>
      </div>
      <RelatedTable
        v-show="showClue"
        ref="clueTableRef"
        :api="GetRepeatClueList"
        :columns="clueColumns"
        :title="t('workbench.duplicateCheck.relatedClues')"
        class="crm-clue-related-table"
        is-return-native-response
      />
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
</template>

<script setup lang="ts">
  import { NButton, NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { validatePhone } from '@lib/shared/method/validate';
  import { RepeatContactItem, RepeatCustomerItem } from '@lib/shared/models/system/business';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import RelatedTable from './components/relatedTable.vue';

  import {
    GetRepeatClueDetailList,
    GetRepeatClueList,
    getRepeatContactList,
    GetRepeatCustomerList,
    GetRepeatOpportunityDetailList,
  } from '@/api/modules';
  // import { clueBaseSteps } from '@/config/clue';
  import { lastOpportunitySteps } from '@/config/opportunity';
  import { hasAnyPermission } from '@/utils/permission';

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
  const keywordVal = computed(() => keyword.value.replace(/[\s\uFEFF\xA0]+/g, ''));

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

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const clueTableRef = ref<InstanceType<typeof RelatedTable>>();
  const repeatAccountTable = useTable(GetRepeatCustomerList, {
    showSetting: false,
    columns,
    isReturnNativeResponse: true,
    crmPagination: {
      size: 'small',
    },
    hiddenTotal: true,
    containerClass: '.crm-repeat-table',
  });

  const repeatContactTable = useTable(getRepeatContactList, {
    showSetting: false,
    columns: contactColumn,
    isReturnNativeResponse: true,
    crmPagination: {
      size: 'small',
    },
    hiddenTotal: true,
    containerClass: '.crm-detail-related-table',
  });

  const repeatTable = ref<Record<string, any>>(repeatAccountTable);

  watch(
    () => keyword.value,
    (val) => {
      const newKeywordVal = val.replace(/[\s\uFEFF\xA0]+/g, '');
      repeatTable.value = validatePhone(newKeywordVal) ? repeatContactTable : repeatAccountTable;
    },
    { immediate: true }
  );

  async function searchData(val: string) {
    repeatTable.value.setLoadListParams({ name: val.replace(/[\s\uFEFF\xA0]+/g, '') });
    repeatTable.value.loadList().finally(() => {
      showResult.value = !!repeatTable.value.propsRes.data.length || repeatTable.value.code === 101003;
      noDuplicateCustomers.value = !showResult.value && !showClue.value;
    });
    clueTableRef.value?.searchData(keywordVal.value).finally(() => {
      showClue.value = !!clueTableRef.value?.propsRes.data.length || clueTableRef.value?.code === 101003;
      noDuplicateCustomers.value = !showResult.value && !showClue.value;
    });
    crmTableRef.value?.scrollTo({ top: 0 });
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
