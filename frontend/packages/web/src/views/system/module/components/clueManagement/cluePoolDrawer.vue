<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :title="t('module.clue.cluePool')"
    :footer="false"
    no-padding
    :show-back="true"
  >
    <div class="h-full w-full bg-[var(--text-n9)] p-[16px]">
      <div class="h-full bg-[var(--text-n10)] p-[16px] pb-0">
        <CrmTable
          v-bind="propsRes"
          class="crm-clue-pool-table"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
        >
          <template #tableTop>
            <n-button class="mb-[16px]" type="primary" @click="handleAdd">
              {{ t('module.clue.addCluePool') }}
            </n-button>
          </template>
        </CrmTable>
      </div>
      <AddOrEditPoolDrawer
        v-model:visible="showAddOrEditDrawer"
        :row="currentRow"
        :type="ModuleConfigEnum.CLUE_MANAGEMENT"
        @refresh="loadList"
      />
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { NButton, NSwitch, useMessage } from 'naive-ui';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddOrEditPoolDrawer from '../addOrEditPoolDrawer.vue';

  import { deleteModuleCluePool, getCluePoolPage, noPickCluePool, switchCluePoolStatus } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const { openModal } = useModal();
  const Message = useMessage();
  const { t } = useI18n();
  const router = useRouter();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const showAddOrEditDrawer = ref<boolean>(false);
  const currentRow = ref<CluePoolItem>();

  // 增加
  function handleAdd() {
    currentRow.value = undefined;
    showAddOrEditDrawer.value = true;
  }

  // 编辑
  async function handleEdit(row: CluePoolItem) {
    currentRow.value = row;
    showAddOrEditDrawer.value = true;
  }

  const tableRefreshId = ref(0);

  async function noPick(id: string) {
    try {
      return await noPickCluePool(id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 删除
  async function handleDelete(row: CluePoolItem) {
    // 判断是否存在未分配的线索
    const hasData = await noPick(row.id);
    const title = hasData
      ? t('module.clue.deleteRulesTitle')
      : t('common.deleteConfirmTitle', { name: characterLimit(row.name) });
    const content = hasData ? '' : t('module.deleteTip', { name: t('module.cluePool') });
    const positiveText = t(hasData ? 'opportunity.gotIt' : 'common.confirm');
    const negativeText = t(hasData ? 'opportunity.goMove' : 'common.cancel');

    openModal({
      type: 'error',
      title,
      content,
      positiveText,
      negativeText,
      positiveButtonProps: {
        type: hasData ? 'primary' : 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        if (!hasData) {
          try {
            await deleteModuleCluePool(row.id);
            Message.success(t('common.deleteSuccess'));
            tableRefreshId.value += 1;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        }
      },
      onNegativeClick: () => {
        if (hasData) {
          visible.value = false;
          router.push({
            name: AppRouteEnum.CLUE_MANAGEMENT_POOL,
          });
        }
      },
    });
  }

  function handleActionSelect(row: CluePoolItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  // 切换状态
  async function handleToggleStatus(row: CluePoolItem) {
    const isEnabling = !row.enable;

    openModal({
      type: isEnabling ? 'default' : 'error',
      title: t(isEnabling ? 'common.confirmEnableTitle' : 'common.confirmDisabledTitle', {
        name: characterLimit(row.name),
      }),
      content: t(isEnabling ? 'module.clue.enabledTipContent' : 'module.clue.disabledTipContent'),
      positiveText: t(isEnabling ? 'common.confirmEnable' : 'common.confirmDisable'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await switchCluePoolStatus(row.id);
          Message.success(t(isEnabling ? 'common.opened' : 'common.disabled'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('module.clue.name'),
      key: 'name',
      width: 300,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      fixed: 'left',
      columnSelectorDisabled: true,
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 120,
      sortOrder: false,
      sorter: true,
      filterOptions: [
        {
          label: t('common.enable'),
          value: true,
        },
        {
          label: t('common.disable'),
          value: false,
        },
      ],
      filter: true,
      render: (row: CluePoolItem) => {
        return h(NSwitch, {
          value: row.enable,
          onClick: () => {
            if (!hasAnyPermission(['MODULE_SETTING:UPDATE'])) return;
            handleToggleStatus(row);
          },
        });
      },
    },
    {
      title: t('opportunity.admin'),
      key: 'owners',
      width: 200,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('role.member'),
      key: 'members',
      width: 200,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('module.clue.autoRecycle'),
      key: 'auto',
      width: 150,
      sortOrder: false,
      sorter: true,
      filterOptions: [
        {
          label: t('common.yes'),
          value: true,
        },
        {
          label: t('common.no'),
          value: false,
        },
      ],
      filter: true,
      render: (row: CluePoolItem) => {
        return row.auto ? t('common.yes') : t('common.no');
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.creator'),
      key: 'createUserName',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUserName',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      key: 'operation',
      width: 120,
      fixed: 'right',
      render: (row: CluePoolItem) =>
        h(CrmOperationButton, {
          groupList: [
            {
              label: t('common.edit'),
              key: 'edit',
            },
            {
              label: t('common.delete'),
              key: 'delete',
            },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getCluePoolPage, {
    tableKey: TableKeyEnum.MODULE_CLUE_POOL,
    showSetting: true,
    columns,
    containerClass: '.crm-clue-pool-table',
  });

  watch(
    () => tableRefreshId.value,
    () => {
      setLoadListParams({});
      loadList();
    }
  );

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        setLoadListParams({});
        loadList();
      }
    }
  );
</script>
