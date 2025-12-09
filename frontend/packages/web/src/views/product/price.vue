<template>
  <div ref="priceCardRef" class="h-full">
    <CrmCard hide-footer no-content-padding>
      <div class="h-full px-[16px] pt-[16px]">
        <CrmTable
          ref="crmTableRef"
          v-model:checked-row-keys="checkedRowKeys"
          v-bind="propsRes"
          :action-config="actionConfig"
          :fullscreen-target-ref="priceCardRef"
          :draggable="hasAnyPermission(['PRICE:UPDATE'])"
          class="crm-price-table"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="filterChange"
          @batch-action="handleBatchAction"
          @drag="dragHandler"
          @refresh="searchData"
        >
          <template #actionLeft>
            <div class="flex items-center gap-[12px]">
              <n-button v-if="hasAnyPermission(['PRICE:ADD'])" type="primary" @click="handleCreate">
                {{ t('product.newPrice') }}
              </n-button>
              <CrmImportButton
                v-if="hasAnyPermission(['PRICE:IMPORT'])"
                :api-type="FormDesignKeyEnum.PRICE"
                :title="t('module.productManagementPrice')"
                @import-success="() => searchData()"
              />
              <n-button
                v-if="hasAnyPermission(['PRICE:EXPORT'])"
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
              :search-placeholder="t('common.searchByName')"
              :custom-fields-config-list="customFieldsFilterConfig"
              :filter-config-list="filterConfigList"
              @adv-search="handleAdvSearch"
              @keyword-search="searchByKeyword"
            />
          </template>
        </CrmTable>
      </div>
    </CrmCard>
  </div>
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="FormDesignKeyEnum.PRICE"
    :other-save-params="otherFollowRecordSaveParams"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    @saved="() => searchData()"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="price"
    @create-success="() => (checkedRowKeys = [])"
  />
  <CrmBatchEditModal
    v-model:visible="showEditModal"
    v-model:field-list="fieldList"
    :ids="checkedRowKeys"
    :form-key="FormDesignKeyEnum.PRICE"
    @refresh="handleRefresh"
  />
  <priceDetailDrawer :id="activeSourceId" v-model:show="showPriceDetailDrawer" @edit="handleEdit" />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem, TableDraggedParams } from '@lib/shared/models/common';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import priceDetailDrawer from './components/priceDetailDrawer.vue';

  import { deleteProductPrice, dragSortProductPrice } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const priceCardRef = ref<HTMLElement | null>(null);
  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref<string>('');
  const needInitDetail = ref(false);
  const otherFollowRecordSaveParams = ref<Record<string, any>>({ id: '' });
  const tableRefreshId = ref(0);
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const actionConfig = {
    baseAction: [
      {
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: ['PRICE:EXPORT'],
      },
      {
        label: t('common.batchEdit'),
        key: 'batchEdit',
        permission: ['PRICE:UPDATE'],
      },
    ],
  };
  const handleSearchData = ref<null | ((...args: any[]) => void)>(null);
  const keyword = ref<string>('');
  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [...baseFilterConfigList] as FilterFormItem[];
  });

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  // 编辑
  function handleEdit(id: string) {
    otherFollowRecordSaveParams.value.id = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  // 删除
  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('product.deletePriceContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteProductPrice(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: any, actionKey: string, done?: () => void) {
    activeSourceId.value = row.id;
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const showPriceDetailDrawer = ref(false);

  const { useTableRes, customFieldsFilterConfig, fieldList } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.PRICE,
    excludeFieldIds: ['customerId'],
    containerClass: 'crm-price-table',
    operationColumn: {
      key: 'operation',
      width: 120,
      fixed: 'right',
      render: (row: any) =>
        h(CrmOperationButton, {
          groupList: [
            {
              label: t('common.edit'),
              key: 'edit',
              permission: ['PRICE:UPDATE'],
            },
            {
              label: t('common.delete'),
              key: 'delete',
              permission: ['PRICE:DELETE'],
            },
          ],
          onSelect: (key: string, done?: () => void) => handleActionSelect(row, key, done),
        }),
    },
    specialRender: {
      name: (row: any) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.id;
              showPriceDetailDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    permission: ['PRICE:UPDATE', 'PRICE:DELETE'],
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  function filterChange(val: any) {
    propsEvent.value.filterChange(val);
  }

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  function searchData(_keyword?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  handleSearchData.value = searchData;
  function handleAdvSearch(filter: FilterResult) {
    keyword.value = '';
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  // 拖拽
  async function dragHandler(params: TableDraggedParams) {
    try {
      await dragSortProductPrice(params);
      Message.success(t('common.operationSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const showEditModal = ref(false);
  function handleBatchEdit() {
    showEditModal.value = true;
  }

  const showExportModal = ref<boolean>(false);
  const isExportAll = ref(false);
  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });
  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[])
  );

  function handleExportAllClick() {
    showExportModal.value = true;
    isExportAll.value = true;
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchEdit':
        handleBatchEdit();
        break;
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  function handleCreate() {
    needInitDetail.value = false;
    formCreateDrawerVisible.value = true;
  }

  watch(
    () => tableRefreshId.value,
    () => {
      crmTableRef.value?.clearCheckedRowKeys();
      searchData();
    }
  );

  onBeforeMount(() => {
    searchData();
  });
</script>

<style lang="less" scoped></style>
