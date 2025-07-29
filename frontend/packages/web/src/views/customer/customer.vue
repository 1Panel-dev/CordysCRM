<template>
  <div ref="customerCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full p-[16px] !pb-0">
        <CrmTable
          ref="crmTableRef"
          v-model:checked-row-keys="checkedRowKeys"
          v-bind="propsRes"
          :columns="tableColumns"
          :not-show-table-filter="isAdvancedSearchMode"
          :action-config="actionConfig"
          :fullscreen-target-ref="customerCardRef"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
          @batch-action="handleBatchAction"
        >
          <template #actionLeft>
            <div class="flex items-center gap-[12px]">
              <n-button
                v-if="activeTab !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION"
                v-permission="['CUSTOMER_MANAGEMENT:ADD']"
                type="primary"
                @click="handleNewClick"
              >
                {{ t('customer.new') }}
              </n-button>
              <n-button
                v-if="
                  hasAnyPermission(['CUSTOMER_MANAGEMENT:EXPORT']) &&
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
              :type="FormDesignKeyEnum.CUSTOMER"
              :internal-list="tabList"
              :custom-fields-config-list="filterConfigList"
              :filter-config-list="customFieldsFilterConfig"
            />
          </template>
        </CrmTable>
      </div>
    </CrmCard>
  </div>
  <TransferModal
    v-model:show="showTransferModal"
    :source-ids="checkedRowKeys"
    :save-api="batchTransferCustomer"
    @load-list="searchData"
  />
  <customerOverviewDrawer v-model:show="showOverviewDrawer" :source-id="activeSourceId" @saved="searchData" />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :other-save-params="otherFollowRecordSaveParams"
    @saved="searchData"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="customer"
    @create-success="handleExportCreateSuccess"
  />
  <CrmMoveModal
    v-model:show="showMoveModal"
    :reason-key="ReasonTypeEnum.CUSTOMER_POOL_RS"
    :source-id="moveIds"
    :name="initialSourceName"
    type="warning"
    @refresh="() => (tableRefreshId += 1)"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, TabPaneProps, useMessage } from 'naive-ui';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum, ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { DeptUserTreeNode } from '@lib/shared/models/system/role';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import { CrmPopConfirmIconType } from '@/components/pure/crm-pop-confirm/index.vue';
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
  import customerOverviewDrawer from './components/customerOverviewDrawer.vue';

  import {
    batchDeleteCustomer,
    batchTransferCustomer,
    deleteCustomer,
    getFieldDeptTree,
    updateCustomer,
  } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useHiddenTab from '@/hooks/useHiddenTab';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const allTabList: TabPaneProps[] = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('customer.all'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('customer.mine'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('customer.deptCustomer'),
    },
    {
      name: CustomerSearchTypeEnum.CUSTOMER_COLLABORATION,
      tab: t('customer.cooperationCustomer'),
    },
  ];
  const { tabList, activeTab } = useHiddenTab(allTabList, FormDesignKeyEnum.CUSTOMER);

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.CUSTOMER);
  const otherFollowRecordSaveParams = ref({
    type: 'CUSTOMER',
    customerId: '',
    id: '',
  });

  function handleNewClick() {
    needInitDetail.value = false;
    activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
    activeSourceId.value = '';
    formCreateDrawerVisible.value = true;
  }

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: ['CUSTOMER_MANAGEMENT:EXPORT'],
      },
      {
        label: t('common.batchTransfer'),
        key: 'batchTransfer',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      {
        label: t('customer.moveToOpenSea'),
        key: 'moveToOpenSea',
        permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
        permission: ['CUSTOMER_MANAGEMENT:DELETE'],
      },
    ],
  };

  const tableRefreshId = ref(0);

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('customer.batchDeleteTitleTip', { number: checkedRowKeys.value.length }),
      content: t('customer.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchDeleteCustomer(checkedRowKeys.value);
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const showMoveModal = ref(false);
  const moveIds = ref<(string | number) | (string | number)[]>('');

  function handleMoveToOpenSea(row?: any) {
    initialSourceName.value = row?.name ?? '';
    moveIds.value = row?.id ? row.id : checkedRowKeys.value;
    showMoveModal.value = true;
  }

  // 批量转移
  const showTransferModal = ref<boolean>(false);
  const showExportModal = ref<boolean>(false);

  const isExportAll = ref(false);
  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchTransfer':
        showTransferModal.value = true;
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      case 'moveToOpenSea':
        handleMoveToOpenSea();
        break;
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  // 删除
  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('customer.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomer(row.id);
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
  const transferForm = ref<any>({
    owner: null,
  });

  async function transferCustomer() {
    try {
      transferLoading.value = true;
      await updateCustomer({
        id: activeSourceId.value,
        owner: transferForm.value.owner,
      });

      Message.success(t('common.transferSuccess'));
      transferForm.value.owner = null;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      transferLoading.value = false;
    }
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
        activeSourceId.value = row.id;
        needInitDetail.value = true;
        otherFollowRecordSaveParams.value.id = row.id;
        formCreateDrawerVisible.value = true;
        break;
      case 'followUp':
        activeFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER;
        activeSourceId.value = row.id;
        needInitDetail.value = false;
        initialSourceName.value = row.name;
        otherFollowRecordSaveParams.value.customerId = row.id;
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-transfer':
        activeSourceId.value = row.id;
        transferCustomer();
        break;
      case 'delete':
        handleDelete(row);
        break;
      case 'moveToOpenSea':
        handleMoveToOpenSea(row);
        break;
      default:
        break;
    }
  }

  const operationGroupList = computed<ActionsItem[]>(() => {
    return [
      {
        label: t('opportunity.followUp'),
        key: 'followUp',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      ...(activeTab.value !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION
        ? [
            {
              label: t('common.edit'),
              key: 'edit',
              permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
            },
            {
              label: t('common.transfer'),
              key: 'transfer',
              popConfirmProps: {
                loading: transferLoading.value,
                title: t('common.transfer'),
                positiveText: t('common.confirm'),
                iconType: 'primary' as CrmPopConfirmIconType,
              },
              popSlotName: 'transferPopTitle',
              popSlotContent: 'transferPopContent',
              permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
            },
            {
              label: 'more',
              key: 'more',
              slotName: 'more',
            },
          ]
        : []),
    ];
  });

  // 概览
  const showOverviewDrawer = ref(false);
  const customerCardRef = ref<HTMLElement | null>(null);

  const { useTableRes, customFieldsFilterConfig, reasonOptions } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CUSTOMER,
    disabledSelection: (row: any) => {
      return row.collaborationType === 'READ_ONLY';
    },
    operationColumn: {
      key: 'operation',
      width: 200,
      fixed: 'right',
      render: (row: any) =>
        ['convertedToCustomer', 'convertedToOpportunity'].includes(activeTab.value) ||
        row.collaborationType === 'READ_ONLY'
          ? '-'
          : h(
              CrmOperationButton,
              {
                groupList: operationGroupList.value,
                moreList: [
                  ...(activeTab.value !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION
                    ? [
                        {
                          label: t('customer.moveToOpenSea'),
                          key: 'moveToOpenSea',
                          permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
                        },
                        {
                          label: t('common.delete'),
                          key: 'delete',
                          danger: true,
                          permission: ['CUSTOMER_MANAGEMENT:DELETE'],
                        },
                      ]
                    : []),
                ],
                onSelect: (key: string) => handleActionSelect(row, key),
                onCancel: () => {
                  transferForm.value.owner = null;
                },
              },
              {
                transferPopContent: () => {
                  return h(TransferForm, {
                    class: 'w-[320px] mt-[16px]',
                    form: transferForm.value,
                    ref: transferFormRef,
                    moduleType: ModuleConfigEnum.CLUE_MANAGEMENT,
                  });
                },
              }
            ),
    },
    specialRender: {
      name: (row: any) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
              activeSourceId.value = row.id;
              showOverviewDrawer.value = true;
            },
          },
          { trigger: () => row.name, default: () => row.name }
        );
      },
      // TODO 缺少字段
      recycleReason: (row: any) => {
        return reasonOptions.value.find((e) => e.value === row.recycleReason)?.label ?? '-';
      },
    },
    permission: ['CUSTOMER_MANAGEMENT:RECYCLE', 'CUSTOMER_MANAGEMENT:UPDATE', 'CUSTOMER_MANAGEMENT:DELETE'],
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;
  const tableColumns = computed(() => {
    if (activeTab.value === CustomerSearchTypeEnum.CUSTOMER_COLLABORATION) {
      return propsRes.value.columns
        .filter((item: any) => item.type !== 'selection')
        .map((e) => {
          if (e.key === 'operation') {
            return {
              ...e,
              width: 80,
            };
          }
          return e;
        });
    }
    return propsRes.value.columns;
  });

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

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }

  const department = ref<DeptUserTreeNode[]>([]);
  async function initDepartList() {
    try {
      department.value = await getFieldDeptTree();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

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
    setLoadListParams({ keyword: val, searchType: activeTab.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

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

  onMounted(() => {
    initDepartList();
  });
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
