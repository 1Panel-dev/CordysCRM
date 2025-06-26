<template>
  <CrmTable
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="actionConfig"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @batch-action="handleBatchAction"
  >
    <template #actionLeft>
      <div class="flex items-center gap-[12px]">
        <n-button
          v-if="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:ADD']) && activeTab !== OpportunitySearchTypeEnum.DEAL"
          type="primary"
          @click="handleCreate"
        >
          {{ t('opportunity.createOpportunity') }}
        </n-button>
        <n-button
          v-if="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:EXPORT'])"
          type="primary"
          ghost
          class="n-btn-outline-primary"
          @click="handleExportAllClick"
        >
          {{ t('common.exportAll') }}
        </n-button>
        <!-- TODO 不上 -->
        <!-- <CrmImportButton
            :validate-api="importUserPreCheck"
            :import-save-api="importUsers"
            :title="t('opportunity.importOpportunity')"
            :button-text="t('opportunity.importOpportunity')"
            @import-success="() => searchData()"
          /> -->
      </div>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        ref="msAdvanceFilterRef"
        v-model:keyword="keyword"
        :search-placeholder="t('opportunity.searchPlaceholder')"
        :custom-fields-config-list="filterConfigList"
        :filter-config-list="customFieldsFilterConfig"
        @adv-search="handleAdvSearch"
        @keyword-search="searchByKeyword"
      />
    </template>
  </CrmTable>
  <TransferModal
    v-model:show="showTransferModal"
    :is-batch="true"
    :source-ids="checkedRowKeys"
    :save-api="transferOpt"
    @load-list="handleRefresh"
  />
  <OptOverviewDrawer v-model:show="showOverviewDrawer" :detail="activeOpportunity" @refresh="handleRefresh" />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="realFormKey"
    :other-save-params="otherFollowRecordSaveParams"
    :source-id="activeSourceId"
    :initial-source-name="initialSourceName"
    :need-init-detail="needInitDetail"
    @saved="searchData"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    type="opportunity"
    @create-success="handleExportCreateSuccess"
  />
  <openSeaOverviewDrawer
    v-model:show="showOpenSeaOverviewDrawer"
    :pool-id="openSea"
    :source-id="activeSourceId"
    @change="searchData"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { OpportunitySearchTypeEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { TransferParams } from '@lib/shared/models/customer/index';
  import type { OpportunityItem } from '@lib/shared/models/opportunity';
  import type { DeptUserTreeNode } from '@lib/shared/models/system/role';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  // TODO 不上
  // import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import OptOverviewDrawer from './optOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { batchDeleteOpt, deleteOpt, getFieldDeptTree, transferOpt } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { defaultTransferForm, lastOpportunitySteps } from '@/config/opportunity';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    activeTab?: OpportunitySearchTypeEnum;
    isCustomerTab?: boolean;
    sourceId?: string; // 客户详情下时传入客户 ID
  }>();
  const emit = defineEmits<{
    (e: 'openCustomerDrawer', activeSourceId: string): void;
  }>();

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);

  const keyword = ref('');
  const tableRefreshId = ref(0);

  const actionConfig = computed<BatchActionConfig>(() => {
    return {
      baseAction: [
        {
          label: t('common.exportChecked'),
          key: 'exportChecked',
          permission: ['OPPORTUNITY_MANAGEMENT:EXPORT'],
        },
        ...(props.activeTab !== OpportunitySearchTypeEnum.DEAL
          ? [
              {
                label: t('common.batchTransfer'),
                key: 'batchTransfer',
                permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
              },
              {
                label: t('common.batchDelete'),
                key: 'batchDelete',
                permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
              },
            ]
          : []),
      ],
    };
  });

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  // 批量转移
  const showTransferModal = ref<boolean>(false);
  function handleBatchTransfer() {
    showTransferModal.value = true;
  }

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('opportunity.batchDeleteTitleTip', { number: checkedRowKeys.value.length }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchDeleteOpt(checkedRowKeys.value);
          Message.success(t('common.deleteSuccess'));
          handleRefresh();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const showExportModal = ref<boolean>(false);

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchTransfer':
        handleBatchTransfer();
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      case 'exportChecked':
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  const showOverviewDrawer = ref<boolean>(false);
  const activeSourceId = ref('');
  const activeOpportunity = ref<OpportunityItem>();
  const formCreateDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.BUSINESS);
  const initialSourceName = ref('');
  const needInitDetail = ref(false);

  function handleCreate() {
    realFormKey.value = FormDesignKeyEnum.BUSINESS;
    activeSourceId.value = '';
    formCreateDrawerVisible.value = true;
    needInitDetail.value = false;
  }

  function handleExportAllClick() {
    showExportModal.value = true;
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  const otherFollowRecordSaveParams = ref({
    type: 'BUSINESS',
    id: '',
    opportunityId: '',
  });

  // 编辑
  function handleEdit(id: string) {
    realFormKey.value = FormDesignKeyEnum.BUSINESS;
    otherFollowRecordSaveParams.value.id = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  // 跟进

  function handleFollowUp(row: OpportunityItem) {
    realFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS;
    otherFollowRecordSaveParams.value.opportunityId = row.id;
    const { customerName, customerId, name } = row;
    initialSourceName.value = JSON.stringify({
      name,
      customerName,
      customerId,
    });
    needInitDetail.value = false;
    formCreateDrawerVisible.value = true;
  }

  // 删除
  function handleDelete(row: OpportunityItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOpt(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const transferForm = ref<TransferParams>({
    owner: null,
    ids: [],
  });

  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const transferLoading = ref(false);

  // 转移
  function handleTransfer(row: OpportunityItem, done?: () => void) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await transferOpt({
            ...transferForm.value,
            ids: [row.id],
          });
          Message.success(t('common.transferSuccess'));
          transferForm.value = { ...defaultTransferForm };
          tableRefreshId.value += 1;
          done?.();
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          transferLoading.value = false;
        }
      }
    });
  }

  function handleActionSelect(row: OpportunityItem, actionKey: string, done?: () => void) {
    activeSourceId.value = row.id;
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'followUp':
        handleFollowUp(row);
        break;
      case 'pop-transfer':
        handleTransfer(row, done);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  function getOperationGroupList(row: OpportunityItem): ActionsItem[] {
    const transferAction: ActionsItem[] = [
      {
        label: t('common.transfer'),
        key: 'transfer',
        popConfirmProps: {
          loading: transferLoading.value,
          title: t('common.transfer'),
          positiveText: t('common.confirm'),
          iconType: 'primary',
        },
        popSlotContent: 'transferPopContent',
        permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
      },
    ];

    if (row.stage === StageResultEnum.FAIL) {
      return transferAction;
    }

    if (row.stage === StageResultEnum.SUCCESS) {
      return [];
    }

    return [
      {
        label: t('common.edit'),
        key: 'edit',
        permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
      },
      {
        label: t('opportunity.followUp'),
        key: 'followUp',
        permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
      },
      ...transferAction,
      {
        label: t('common.delete'),
        key: 'delete',
        permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
      },
    ];
  }

  const showOpenSeaOverviewDrawer = ref<boolean>(false);
  const openSea = ref<string | number>('');

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: props.isCustomerTab ? FormDesignKeyEnum.CUSTOMER_OPPORTUNITY : FormDesignKeyEnum.BUSINESS,
    excludeFieldIds: ['customerId'],
    operationColumn: {
      key: 'operation',
      width: 200,
      fixed: 'right',
      render: (row: OpportunityItem) =>
        row.stage === StageResultEnum.SUCCESS
          ? '-'
          : h(
              CrmOperationButton,
              {
                groupList: getOperationGroupList(row),
                onSelect: (key: string, done?: () => void) => handleActionSelect(row, key, done),
                onCancel: () => {
                  transferForm.value = { ...defaultTransferForm };
                },
              },
              {
                transferPopContent: () => {
                  return h(TransferForm, {
                    class: 'w-[320px] mt-[16px]',
                    form: transferForm.value,
                    ref: transferFormRef,
                  });
                },
              }
            ),
    },
    specialRender: {
      name: (row: OpportunityItem) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.id;
              activeOpportunity.value = row;
              realFormKey.value = FormDesignKeyEnum.BUSINESS;
              showOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
      customerId: (row: OpportunityItem) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.customerId;
              if (row.inCustomerPool) {
                openSea.value = row.poolId ?? '';
                showOpenSeaOverviewDrawer.value = true;
              } else {
                emit('openCustomerDrawer', activeSourceId.value);
              }
            },
          },
          { default: () => row.customerName, trigger: () => row.customerName }
        );
      },
      status: (row: OpportunityItem) => {
        return row.status ? t('common.open') : t('common.close');
      },
      stage: (row: OpportunityItem) => {
        const step = lastOpportunitySteps.find((e: any) => e.value === row.stage);
        return step ? step.label : '-';
      },
    },
    permission: ['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:DELETE'],
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const exportColumns = computed<ExportTableColumnItem[]>(() => {
    return propsRes.value.columns
      .filter((item: any) => item.key !== 'operation' && item.type !== 'selection')
      .map((e) => {
        return {
          key: e.key?.toString() || '',
          title: (e.title as string) || '',
        };
      });
  });

  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });

  function handleAdvSearch(filter: FilterResult) {
    keyword.value = '';
    setAdvanceFilter(filter);
    loadList();
  }

  const msAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  const isAdvancedSearchMode = computed(() => msAdvanceFilterRef.value?.isAdvancedSearchMode);

  const department = ref<DeptUserTreeNode[]>([]);

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('opportunity.opportunityStage'),
        dataIndex: 'stage',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options: lastOpportunitySteps,
        },
      },
      {
        title: t('opportunity.department'),
        dataIndex: 'departmentId',
        type: FieldTypeEnum.TREE_SELECT,
        treeSelectProps: {
          labelField: 'name',
          keyField: 'id',
          multiple: true,
          clearFilterAfterSelect: false,
          options: department.value,
          checkable: true,
        },
      },
      {
        title: t('customer.lastFollowUpDate'),
        dataIndex: 'followTime',
        type: FieldTypeEnum.DATE_TIME,
      },
      ...baseFilterConfigList,
    ];
  });

  function searchData() {
    setLoadListParams({
      keyword: keyword.value,
      searchType: props.activeTab,
      customerId: props.sourceId,
    });
    loadList();
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  watch(
    () => props.activeTab,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        searchData();
      }
    },
    { immediate: true }
  );

  watch(
    () => tableRefreshId.value,
    () => {
      searchData();
    }
  );

  async function initDepartList() {
    try {
      department.value = await getFieldDeptTree();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initDepartList();
    if (props.isCustomerTab) {
      searchData();
    }
  });
</script>

<style lang="less" scoped></style>
