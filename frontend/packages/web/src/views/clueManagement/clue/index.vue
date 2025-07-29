<template>
  <div ref="leadCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full p-[16px] !pb-0">
        <CrmTable
          ref="crmTableRef"
          v-model:checked-row-keys="checkedRowKeys"
          v-bind="propsRes"
          :not-show-table-filter="isAdvancedSearchMode"
          :action-config="actionConfig"
          :fullscreen-target-ref="leadCardRef"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
          @batch-action="handleBatchAction"
        >
          <template #actionLeft>
            <div class="flex items-center gap-[12px]">
              <n-button v-permission="['CLUE_MANAGEMENT:ADD']" type="primary" @click="handleAdd">
                {{ t('clueManagement.newClue') }}
              </n-button>
              <n-button
                v-if="
                  hasAnyPermission(['CLUE_MANAGEMENT:EXPORT']) &&
                  activeTab !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION
                "
                type="primary"
                ghost
                class="n-btn-outline-primary"
                :disabled="propsRes.data.length === 0"
                @click="handleExportAllClick"
              >
                {{ t('common.exportAll') }}
              </n-button>
            </div>
          </template>
          <template #actionRight>
            <CrmAdvanceFilter
              ref="tableAdvanceFilterRef"
              v-model:keyword="keyword"
              :custom-fields-config-list="filterConfigList"
              :filter-config-list="customFieldsFilterConfig"
              @adv-search="handleAdvSearch"
              @keyword-search="searchData"
            />
          </template>
          <template #view>
            <CrmViewSelect
              v-model:active-tab="activeTab"
              :type="FormDesignKeyEnum.CLUE"
              :internal-list="tabList"
              :custom-fields-config-list="filterConfigList"
              :filter-config-list="customFieldsFilterConfig"
            />
          </template>
        </CrmTable>
      </div>
    </CrmCard>
    <TransferModal
      v-model:show="showTransferModal"
      :source-ids="checkedRowKeys"
      :save-api="batchTransferClue"
      @load-list="handleRefresh"
    />
    <ClueOverviewDrawer
      v-if="isInitOverviewDrawer"
      v-model:show="showOverviewDrawer"
      :detail="activeClue"
      @refresh="handleRefresh"
      @convert-to-customer="() => handleConvertToCustomer(activeClue)"
    />
    <CrmFormCreateDrawer
      v-if="isInitFormCreateDrawer"
      v-model:visible="formCreateDrawerVisible"
      :form-key="formKey"
      :source-id="activeClueId"
      :need-init-detail="needInitDetail"
      :initial-source-name="activeRowName"
      :other-save-params="otherFollowRecordSaveParams"
      @saved="loadList"
    />
  </div>
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="clue"
    @create-success="handleExportCreateSuccess"
  />
  <convertToCustomerDrawer
    v-if="isInitConvertDrawer"
    v-model:show="showConvertToCustomerDrawer"
    :clue-id="otherFollowRecordSaveParams.clueId"
    @new="handleNewCustomer"
    @finish="handleRefresh"
  />
  <CrmMoveModal
    v-model:show="showMoveModal"
    :reason-key="ReasonTypeEnum.CLUE_POOL_RS"
    :source-id="moveIds"
    :name="activeRowName"
    @refresh="() => handleRefresh()"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, TabPaneProps, useMessage } from 'naive-ui';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  // import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { ClueListItem } from '@lib/shared/models/clue';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { TransferParams } from '@lib/shared/models/customer/index';
  import { DeptUserTreeNode } from '@lib/shared/models/system/role';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';

  import { batchDeleteClue, batchTransferClue, deleteClue, getFieldDeptTree } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { defaultTransferForm } from '@/config/opportunity';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useHiddenTab from '@/hooks/useHiddenTab';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const convertToCustomerDrawer = defineAsyncComponent(() => import('./components/convertToCustomerDrawer.vue'));
  const ClueOverviewDrawer = defineAsyncComponent(() => import('./components/clueOverviewDrawer.vue'));

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const allTabList: TabPaneProps[] = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('clue.allClues'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('clue.myClues'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('clue.departmentClues'),
    },
  ];

  const leadCardRef = ref<HTMLElement | null>(null);

  const { tabList, activeTab } = useHiddenTab(allTabList, FormDesignKeyEnum.CLUE);

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const activeClueId = ref('');
  const needInitDetail = ref(false);
  const activeClue = ref<ClueListItem>();
  const formKey = ref(FormDesignKeyEnum.CLUE);
  const isInitFormCreateDrawer = ref(false);
  const formCreateDrawerVisible = ref(false);

  const actionConfig = computed<BatchActionConfig>(() => {
    return {
      baseAction: [
        {
          label: t('common.exportChecked'),
          key: 'exportChecked',
          permission: ['CLUE_MANAGEMENT:EXPORT'],
        },
        ...(activeTab.value !== CustomerSearchTypeEnum.CUSTOMER_TRANSITION
          ? [
              {
                label: t('common.batchTransfer'),
                key: 'batchTransfer',
                permission: ['CLUE_MANAGEMENT:UPDATE'],
              },
              {
                label: t('clue.moveIntoCluePool'),
                key: 'moveIntoCluePool',
                permission: ['CLUE_MANAGEMENT:RECYCLE'],
              },
              {
                label: t('common.batchDelete'),
                key: 'batchDelete',
                permission: ['CLUE_MANAGEMENT:DELETE'],
              },
            ]
          : []),
      ],
    };
  });

  const tableRefreshId = ref(0);

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  const showExportModal = ref<boolean>(false);
  const activeRowName = ref('');
  // 移入线索池
  const showMoveModal = ref(false);
  const moveIds = ref<(string | number) | (string | number)[]>('');
  function handleMoveToLeadPool(row?: ClueListItem) {
    activeRowName.value = row?.name ?? '';
    moveIds.value = row?.id ? row.id : checkedRowKeys.value;
    showMoveModal.value = true;
  }

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('clue.batchDeleteTitleTip', { number: checkedRowKeys.value.length }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchDeleteClue(checkedRowKeys.value as string[]);
          handleRefresh();
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 批量转移
  const showTransferModal = ref<boolean>(false);
  const isExportAll = ref(false);

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchTransfer':
        showTransferModal.value = true;
        break;
      case 'moveIntoCluePool':
        handleMoveToLeadPool();
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  // 删除
  function handleDelete(row: ClueListItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteClue(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 转移
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const transferLoading = ref(false);
  const transferForm = ref<TransferParams>({
    ...defaultTransferForm,
  });

  function handleTransfer(row: ClueListItem) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await batchTransferClue({
            ...transferForm.value,
            ids: [row.id],
          });
          Message.success(t('common.transferSuccess'));
          transferForm.value = { ...defaultTransferForm };
          tableRefreshId.value += 1;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          transferLoading.value = false;
        }
      }
    });
  }

  // 新增
  function handleAdd() {
    isInitFormCreateDrawer.value = true;
    formKey.value = FormDesignKeyEnum.CLUE;
    activeClueId.value = '';
    formCreateDrawerVisible.value = true;
  }

  function handleNewCustomer() {
    isInitFormCreateDrawer.value = true;
    formKey.value = FormDesignKeyEnum.CUSTOMER;
    activeClueId.value = '';
    formCreateDrawerVisible.value = true;
  }

  const isInitConvertDrawer = ref(false);
  const showConvertToCustomerDrawer = ref(false);

  const otherFollowRecordSaveParams = ref({
    type: 'CLUE',
    clueId: '',
    id: '',
  });

  function handleConvertToCustomer(row?: ClueListItem) {
    isInitConvertDrawer.value = true;
    activeClueId.value = '';
    formKey.value = FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER;
    activeRowName.value = row?.name || '';
    otherFollowRecordSaveParams.value.clueId = row?.id || '';
    needInitDetail.value = false;
    showConvertToCustomerDrawer.value = true;
  }

  function handleActionSelect(row: ClueListItem, actionKey: string) {
    activeClueId.value = row.id;
    switch (actionKey) {
      case 'edit':
        isInitFormCreateDrawer.value = true;
        otherFollowRecordSaveParams.value.id = row.id;
        needInitDetail.value = true;
        formKey.value = FormDesignKeyEnum.CLUE;
        formCreateDrawerVisible.value = true;
        break;
      case 'followUp':
        isInitFormCreateDrawer.value = true;
        formKey.value = FormDesignKeyEnum.FOLLOW_RECORD_CLUE;
        otherFollowRecordSaveParams.value.clueId = row.id;
        activeRowName.value = row.name;
        needInitDetail.value = false;
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-transfer':
        handleTransfer(row);
        break;
      case 'convertToCustomer':
        activeClueId.value = '';
        // formKey.value = FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER;
        // activeRowName.value = row.name;
        // otherFollowRecordSaveParams.value.clueId = row.id;
        // needInitDetail.value = false;
        // showConvertToCustomerDrawer.value = true;
        handleConvertToCustomer(row);
        break;
      case 'moveIntoCluePool':
        handleMoveToLeadPool(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  // 概览
  const isInitOverviewDrawer = ref(false);
  const showOverviewDrawer = ref(false);

  const { useTableRes, customFieldsFilterConfig, reasonOptions } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CLUE,
    operationColumn: {
      key: 'operation',
      width: 200,
      fixed: 'right',
      render: (row: ClueListItem) =>
        row.transitionType && ['CUSTOMER', 'OPPORTUNITY'].includes(row.transitionType)
          ? '-'
          : h(
              CrmOperationButton,
              {
                groupList: [
                  {
                    label: t('common.edit'),
                    key: 'edit',
                    permission: ['CLUE_MANAGEMENT:UPDATE'],
                  },
                  {
                    label: t('opportunity.followUp'),
                    key: 'followUp',
                    permission: ['CLUE_MANAGEMENT:UPDATE'],
                  },
                  {
                    label: t('common.transfer'),
                    key: 'transfer',
                    permission: ['CLUE_MANAGEMENT:UPDATE'],
                    popConfirmProps: {
                      loading: transferLoading.value,
                      title: t('common.transfer'),
                      positiveText: t('common.confirm'),
                      iconType: 'primary',
                    },
                    popSlotContent: 'transferPopContent',
                  },
                  {
                    label: 'more',
                    key: 'more',
                    slotName: 'more',
                  },
                ],
                moreList: [
                  {
                    label: t('clue.moveIntoCluePool'),
                    key: 'moveIntoCluePool',
                    permission: ['CLUE_MANAGEMENT:RECYCLE'],
                  },
                  {
                    label: t('clue.convertToCustomer'),
                    key: 'convertToCustomer',
                    permission: ['CLUE_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT:ADD'],
                    allPermission: true,
                  },
                  ...([CustomerSearchTypeEnum.DEPARTMENT, CustomerSearchTypeEnum.SELF].includes(activeTab.value)
                    ? []
                    : [
                        {
                          label: t('common.delete'),
                          key: 'delete',
                          danger: true,
                          permission: ['CLUE_MANAGEMENT:DELETE'],
                        },
                      ]),
                ],
                onSelect: (key: string) => handleActionSelect(row, key),
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
      name: (row: ClueListItem) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeClue.value = row;
              isInitOverviewDrawer.value = true;
              showOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
      // TODO 缺少字段
      recycleReason: (row: any) => {
        return reasonOptions.value.find((e) => e.value === row.recycleReason)?.label ?? '-';
      },
    },
    permission: ['CLUE_MANAGEMENT:RECYCLE', 'CLUE_MANAGEMENT:DELETE', 'CLUE_MANAGEMENT:UPDATE'],
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const exportColumns = computed<ExportTableColumnItem[]>(() => {
    return propsRes.value.columns
      .filter(
        (item: any) =>
          item.key !== 'operation' &&
          item.type !== 'selection' &&
          item.key !== 'crmTableOrder' &&
          item.filedType !== FieldTypeEnum.PICTURE
      )
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

  const department = ref<DeptUserTreeNode[]>([]);
  const filterConfigList = computed<FilterFormItem[]>(() => [
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
      title: t('customer.lastFollowUps'),
      dataIndex: 'follower',
      type: FieldTypeEnum.USER_SELECT,
    },
    {
      title: t('customer.lastFollowUpDate'),
      dataIndex: 'followTime',
      type: FieldTypeEnum.TIME_RANGE_PICKER,
    },
    {
      title: t('customer.recycleReason'),
      dataIndex: 'recycleReason',
      type: FieldTypeEnum.SELECT,
      selectProps: {
        options: reasonOptions.value,
      },
    },
    ...baseFilterConfigList,
  ]);

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  function handleAdvSearch(filter: FilterResult) {
    keyword.value = '';
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  const isAdvancedSearchMode = computed(() => tableAdvanceFilterRef.value?.isAdvancedSearchMode);

  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

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
  });

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        searchData();
      }
    }
  );

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
