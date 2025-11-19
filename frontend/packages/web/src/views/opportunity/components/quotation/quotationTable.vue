<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :class="`crm-opportunity-table-${props.formKey}`"
    :action-config="actionConfig"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @batch-action="handleBatchAction"
  >
    <template #actionLeft>
      <div class="flex items-center gap-[12px]">
        <n-button v-if="!props.readonly" type="primary" @click="handleCreate">
          {{ t('opportunity.quotation.new') }}
        </n-button>
      </div>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :search-placeholder="t('opportunity.quotation.searchPlaceholder')"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="[]"
        @adv-search="handleAdvSearch"
        @keyword-search="searchByKeyword"
      />
    </template>
  </CrmTable>
</template>
<script setup lang="ts">
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useLocalForage from '@/hooks/useLocalForage';
  import useModal from '@/hooks/useModal';
  import { useUserStore } from '@/store';
  import quotationStatus from './quotationStatus.vue';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';

  const { openModal } = useModal();
  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);

  const useStore = useUserStore();
  const { setItem, getItem } = useLocalForage();

  const props = defineProps<{
    // TODO 商机详情预留 TODO
    formKey: FormDesignKeyEnum.OPPORTUNITY_QUOTATION | FormDesignKeyEnum.BUSINESS;
    sourceId?: string; // 客户详情下时传入客户 ID
    readonly?: boolean;
    openseaHiddenColumns?: string[];
  }>();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchApproval'),
        key: 'approval',
      },
      {
        label: t('common.batchVoid'),
        key: 'invalid',
      },
    ],
  };

  // 批量审批
  function handleBatchApproval() {}
  // 批量作废
  function handleBatchInvalid() {
    openModal({
      type: 'error',
      title: t('opportunity.quotation.batchInvalidTitleTip', { number: checkedRowKeys.value.length }),
      content: t('opportunity.quotation.invalidContentTip'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          Message.success(t('common.voidSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'approval':
        handleBatchApproval();
        break;
      case 'invalid':
        handleBatchInvalid();
        break;
      default:
        break;
    }
  }

  async function handleCreate() {}

  function handleEdit(id: string) {}
  function handleVoid(row: any) {
    openModal({
      type: 'error',
      title: t('opportunity.quotation.voidTitleTip', { name: row.name }),
      content: t('opportunity.quotation.invalidContentTip'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          Message.success(t('common.voidSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const showDetailDrawer = ref(false);
  function handleDelete(row: any) {}
  function handleRevoke(row: any) {}

  function handleActionSelect(row: any, actionKey: string, done?: () => void) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'invalid':
        handleVoid(row);
        break;
      case 'revoke':
        handleRevoke(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const groupList = [
    {
      label: t('common.edit'),
      key: 'edit',
    },
    {
      label: t('common.invalid'),
      key: 'invalid',
    },
    {
      label: t('common.delete'),
      key: 'delete',
    },
  ];

  const moreGroupList = [
    { label: t('common.download'), key: 'download' },
    { label: t('common.revoke'), key: 'revoke' },
  ];

  function getOperationGroupList(row: any) {
    const allGroups = [...groupList, ...moreGroupList];
    const getGroups = (keys: string[]) => allGroups.filter((e) => keys.includes(e.key));
    const commonGroups = ['invalid', 'delete'];

    switch (row.status) {
      case QuotationStatusEnum.SUCCESS:
        return getGroups(['download', ...commonGroups]);
      case QuotationStatusEnum.FAIL:
      case QuotationStatusEnum.REVOKE:
        return getGroups(['edit', ...commonGroups]);
      case QuotationStatusEnum.REVIEW:
        return getGroups(['revoke', ...commonGroups]);
      case QuotationStatusEnum.INVALID:
        return getGroups(['delete']);
      default:
        return [];
    }
  }
  const { useTableRes, customFieldsFilterConfig, reasonOptions, fieldList } = await useFormCreateTable({
    formKey: props.formKey,
    excludeFieldIds: ['customerId'],
    containerClass: `.crm-opportunity-table-${props.formKey}`,
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: 200,
          fixed: 'right',
          render: (row: any) =>
            getOperationGroupList(row).length
              ? h(CrmOperationButton, {
                  groupList: getOperationGroupList(row),
                  onSelect: (key: string, done?: () => void) => handleActionSelect(row, key, done),
                })
              : '-',
        },
    specialRender: {
      name: (row: any) => {
        const createNameButton = () =>
          h(
            CrmTableButton,
            {
              onClick: () => {
                showDetailDrawer.value = true;
              },
            },
            { default: () => row.name, trigger: () => row.name }
          );
        return props.readonly ? h(CrmNameTooltip, { text: row.name }) : createNameButton();
      },
      status: (row: any) =>
        h(quotationStatus, {
          status: row.status,
        }),
    },
    permission: ['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:DELETE'],
    readonly: props.readonly,
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter, filterItem, advanceFilter } =
    useTableRes;

  const isAdvancedSearchMode = ref(false);
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(_keyword?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  onBeforeMount(() => {
    searchData();
  });
</script>

<style scoped></style>
