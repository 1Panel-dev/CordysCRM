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
    :readonly="readonly"
    :row="detail"
    :config-list="props.configList"
    :custom-list="props.customList"
    @refresh="tableRefreshId += 1"
  />
</template>

<script setup lang="ts">
  import { NButton, NSwitch } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddOrEditViewsDrawer from './addOrEditViewsDrawer.vue';

  import { getOpportunityRuleList } from '@/api/modules';

  const { t } = useI18n();

  const props = defineProps<{
    configList: FilterFormItem[]; // 系统字段
    customList?: FilterFormItem[]; // 自定义字段
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const loading = ref<boolean>(false);
  const tableRefreshId = ref(0);

  const addOrEditViewsDrawerVisible = ref(false);
  const readonly = ref(false);
  const detail = ref();

  function handleAdd() {
    readonly.value = false;
    detail.value = undefined;
    addOrEditViewsDrawerVisible.value = true;
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'readOnly':
        readonly.value = true;
        detail.value = row;
        addOrEditViewsDrawerVisible.value = true;
        break;
      case 'edit':
        readonly.value = false;
        detail.value = row;
        addOrEditViewsDrawerVisible.value = true;
        break;
      case 'copy':
        readonly.value = false;
        // TODO lmy
        detail.value = {
          name: `${row.name}copy`,
          condition: row.condition,
        };
        addOrEditViewsDrawerVisible.value = true;
        break;
      case 'delete':
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
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 100,
      render: (row) => {
        return h(NSwitch, { value: row.enable, disabled: row.id === 'all' });
      },
    },
    {
      key: 'operation',
      width: 150,
      fixed: 'right',
      render: (row) => {
        if (row.id === '2') {
          return '-';
        }
        return h(CrmOperationButton, {
          groupList: [
            {
              label: t('crmViewSelect.readOnly'),
              key: 'readOnly',
            },
            ...(row.type === 'internalViews'
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
            ...(row.type === 'internalViews'
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
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getOpportunityRuleList, {
    showSetting: false,
    columns,
    hiddenAllScreen: true,
  });

  watch(
    () => tableRefreshId.value,
    () => {
      // TODO lmy
      setLoadListParams({});
      loadList();
    }
  );

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        // TODO lmy
        setLoadListParams({});
        loadList();
      }
    }
  );
</script>
