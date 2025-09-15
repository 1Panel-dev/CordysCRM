<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <div :key="tableRefreshIdKey" class="h-full">
      <CrmTable
        ref="crmTableRef"
        v-model:checked-row-keys="checkedRowKeys"
        v-bind="propsRes"
        class="crm-product-table"
        :action-config="actionConfig"
        :draggable="hasAnyPermission(['PRODUCT_MANAGEMENT:UPDATE'])"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @batch-action="handleBatchAction"
        @drag="dragHandler"
        @refresh="searchData"
      >
        <template #actionLeft>
          <div class="flex items-center gap-[12px]">
            <n-button
              v-permission="['PRODUCT_MANAGEMENT:ADD']"
              type="primary"
              @click="
                {
                  activeProductId = '';
                  formCreateDrawerVisible = true;
                }
              "
            >
              {{ t('product.createProduct') }}
            </n-button>
            <!-- TODO 联调 -->
            <CrmImportButton
              v-if="hasAnyPermission(['PRODUCT_MANAGEMENT:IMPORT'])"
              :api-type="FormDesignKeyEnum.PRODUCT"
              :title="t('module.productManagement')"
              @import-success="() => searchData()"
            />
          </div>
        </template>
        <template #actionRight>
          <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
        </template>
      </CrmTable>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.PRODUCT"
      :source-id="activeProductId"
      :need-init-detail="!!activeProductId"
      @saved="() => searchData()"
    />
  </CrmCard>
</template>

<script lang="ts" setup>
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { TableDraggedParams } from '@lib/shared/models/common';
  import type { ProductListItem } from '@lib/shared/models/product';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { batchDeleteProduct, batchUpdateProduct, deleteProduct, dragSortProduct } from '@/api/modules';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();

  const { t } = useI18n();

  const Message = useMessage();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeProductId = ref('');
  const tableRefreshId = ref(0);
  const tableRefreshIdKey = ref(0);

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('product.batchUp'),
        key: 'batchUp',
        permission: ['PRODUCT_MANAGEMENT:UPDATE'],
      },
      {
        label: t('product.batchDown'),
        key: 'batchDown',
        permission: ['PRODUCT_MANAGEMENT:UPDATE'],
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
        permission: ['PRODUCT_MANAGEMENT:DELETE'],
      },
    ],
  };

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('product.batchDeleteTitleTip', { number: checkedRowKeys.value.length }),
      content: t('product.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchDeleteProduct(checkedRowKeys.value);
          checkedRowKeys.value = [];
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 批量上架 | 下架
  async function handleBatchUpOrDown(status: boolean) {
    try {
      const params = {
        ids: checkedRowKeys.value,
        status: status ? '1' : '2',
        moduleFields: [],
        price: null,
      };
      await batchUpdateProduct(params);
      Message.success(t(status ? 'product.batchUpSuccess' : 'product.batchDownSuccess'));
      checkedRowKeys.value = [];
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchUp':
        handleBatchUpOrDown(true);
        break;
      case 'batchDown':
        handleBatchUpOrDown(false);
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      default:
        break;
    }
  }

  // 删除
  function handleDelete(row: ProductListItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('product.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteProduct(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 编辑
  function handleEdit(productId: string) {
    activeProductId.value = productId;
    formCreateDrawerVisible.value = true;
  }

  // 拖拽
  async function dragHandler(params: TableDraggedParams) {
    try {
      await dragSortProduct(params);
      Message.success(t('common.operationSuccess'));
      tableRefreshIdKey.value += 1;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleActionSelect(row: ProductListItem, actionKey: string) {
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

  const operationGroupList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      permission: ['PRODUCT_MANAGEMENT:UPDATE'],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: ['PRODUCT_MANAGEMENT:DELETE'],
    },
  ];

  const { useTableRes } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.PRODUCT,
    containerClass: '.crm-product-table',
    operationColumn: {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row: ProductListItem) =>
        h(CrmOperationButton, {
          groupList: operationGroupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
    specialRender: {
      name: (row: ProductListItem) => {
        return h(CrmNameTooltip, { text: row.name });
      },
    },
    permission: ['PRODUCT_MANAGEMENT:UPDATE', 'PRODUCT_MANAGEMENT:DELETE'],
  });
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTableRes;

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  watch(
    () => tableRefreshId.value,
    () => {
      crmTableRef.value?.clearCheckedRowKeys();
      searchData();
    }
  );

  onMounted(() => {
    searchData();
  });
</script>

<style lang="less" scoped></style>
