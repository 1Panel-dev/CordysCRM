<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="t('crmViewSelect.manageViews')"
    :loading="loading"
    :footer="false"
  >
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @page-size-change="propsEvent.pageSizeChange"
    >
      <template #tableTop>
        <n-button type="primary" @click="handleAdd">
          {{ t('crmViewSelect.newView') }}
        </n-button>
      </template>
    </CrmTable>
  </CrmDrawer>

  <AddOrEditViewsDrawer
    v-model:visible="addOrEditViewsDrawerVisible"
    :type="props.type"
    :readonly="readonly"
    :row="detail"
    :config-list="props.configList"
    :custom-list="props.customList"
    @refresh="(val?: string) => emit('changeActive', val)"
  />
</template>

<script setup lang="ts">
  import { NButton, NSwitch, useMessage } from 'naive-ui';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { ViewItem } from '@lib/shared/models/view';

  import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddOrEditViewsDrawer from './addOrEditViewsDrawer.vue';

  import { TabType } from '@/hooks/useHiddenTab';
  import useModal from '@/hooks/useModal';
  import useViewStore from '@/store/modules/view';

  import { viewApiMap } from '../config';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const viewStore = useViewStore();

  const props = defineProps<{
    type: TabType;
    configList: FilterFormItem[]; // 系统字段
    customList?: FilterFormItem[]; // 自定义字段
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'changeActive', id?: string): void;
    (e: 'handleDeleteOrDisable', id: string): void;
  }>();

  const loading = ref<boolean>(false);

  const addOrEditViewsDrawerVisible = ref(false);
  const readonly = ref(false);
  const detail = ref();

  function handleAdd() {
    readonly.value = false;
    detail.value = undefined;
    addOrEditViewsDrawerVisible.value = true;
  }

  function handleDelete(row: ViewItem) {
    openModal({
      type: 'error',
      title: t('crmViewSelect.deleteConfirmTitle', { name: characterLimit(row.name) }),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await viewApiMap.delete[props.type](row.id);
          Message.success(t('common.deleteSuccess'));
          emit('handleDeleteOrDisable', row.id);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleActionSelect(row: ViewItem, actionKey: string) {
    let res;
    if (actionKey !== 'delete') {
      res = await viewStore.getViewDetail(props.type, row);
    }
    switch (actionKey) {
      case 'readOnly':
        readonly.value = true;
        detail.value = { id: row.id, ...res };
        addOrEditViewsDrawerVisible.value = true;
        break;
      case 'edit':
        readonly.value = false;
        detail.value = { id: row.id, ...res };
        addOrEditViewsDrawerVisible.value = true;
        break;
      case 'copy':
        readonly.value = false;
        detail.value = { ...res, name: `${res?.name}copy` };
        addOrEditViewsDrawerVisible.value = true;
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('crmViewSelect.viewName'),
      key: 'name',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row) =>
        h('div', { class: 'flex items-center gap-[8px] overflow-hidden flex-1' }, [
          h(CrmIcon, {
            type: 'iconicon_move',
            class: 'cursor-pointer text-[var(--text-n4)]',
            size: 12,
          }),
          h(CrmIcon, {
            type: row.fixed ? 'iconicon_pin_filled' : 'iconicon_pin',
            class: `cursor-pointer ${row.fixed ? 'text-[var(--primary-8)]' : 'text-[var(--text-n1)]'}`,
            size: 16,
            onClick: (e: MouseEvent) => {
              e.stopPropagation(); // 阻止事件冒泡，防止影响 select 行为
              viewStore.toggleFixed(props.type, row);
            },
          }),
          h('span', {
            class: 'one-line-text',
            innerText: row.name,
          }),
        ]),
    },
    {
      title: t('crmViewSelect.viewType'),
      key: 'type',
      width: 100,
      render: (row) => {
        return row.type === 'internal' ? t('crmViewSelect.systemView') : t('crmViewSelect.personalView');
      },
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 100,
      render: (row) => {
        return h(NSwitch, {
          value: row.enable,
          disabled: row.id === CustomerSearchTypeEnum.ALL,
          onClick: () => {
            if (row.id === CustomerSearchTypeEnum.ALL) return;
            viewStore.toggleEnabled(props.type, row);
            emit('handleDeleteOrDisable', row.id);
          },
        });
      },
    },
    {
      key: 'operation',
      width: 150,
      fixed: 'right',
      render: (row) => {
        if (row.id === CustomerSearchTypeEnum.CUSTOMER_COLLABORATION) {
          return '-';
        }
        return h(CrmOperationButton, {
          groupList: [
            {
              label: t('crmViewSelect.readOnly'),
              key: 'readOnly',
            },
            ...(row.type !== 'internal'
              ? [
                  {
                    label: t('common.edit'),
                    key: 'edit',
                  },
                ]
              : []),
            {
              label: t('common.copy'),
              key: 'copy',
            },
            ...(row.type !== 'internal'
              ? [
                  {
                    label: t('common.delete'),
                    key: 'delete',
                  },
                ]
              : []),
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        });
      },
    },
  ];

  const { propsRes, propsEvent } = useTable(undefined, {
    showSetting: false,
    columns,
    hiddenAllScreen: true,
  });

  watch(
    [() => viewStore.internalViews, () => viewStore.customViews],
    () => {
      propsRes.value.data = [...viewStore.internalViews, ...viewStore.customViews];
      if (propsRes.value.crmPagination) {
        propsRes.value.crmPagination.itemCount = propsRes.value.data.length;
      }
    },
    { deep: true }
  );
</script>
