<template>
  <CrmCard hide-footer>
    <CrmTable
      v-model:checked-row-keys="checkedRowKeys"
      v-bind="propsRes"
      :action-config="actionConfig"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @batch-action="handleBatchAction"
    >
      <template #actionLeft>
        <div class="flex items-center">
          <n-button
            v-permission="['PRODUCT_MANAGEMENT:ADD']"
            class="mr-[12px]"
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
        </div>
      </template>
      <template #actionRight>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
      </template>
    </CrmTable>
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
  import type { ProductListItem } from '@lib/shared/models/product';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { batchDeleteProduct, batchUpdateProduct, deleteProduct } from '@/api/modules';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';

  const { openModal } = useModal();

  const { t } = useI18n();

  const Message = useMessage();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeProductId = ref('');
  const tableRefreshId = ref(0);

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
      title: t('common.deleteConfirmTitle', { name: row.name }),
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
    operationColumn: {
      key: 'operation',
      width: 150,
      fixed: 'right',
      render: (row: ProductListItem) =>
        h(CrmOperationButton, {
          groupList: operationGroupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTableRes;

  function searchData() {
    setLoadListParams({ keyword: keyword.value });
    loadList();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      searchData();
    }
  );

  onMounted(() => {
    searchData();
  });
</script>

<style lang="less" scoped></style>
