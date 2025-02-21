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
      <div class="h-full bg-[var(--text-n10)] p-[16px]">
        <n-button class="mb-[16px]" type="primary" @click="handleAdd">
          {{ t('module.clue.addCluePool') }}
        </n-button>
        <CrmTable
          v-bind="propsRes"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
        />
      </div>
      <AddOrEditCluePoolDrawer v-model:visible="showAddOrEditDrawer" v-model:row="currentRow" @refresh="loadList" />
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NSwitch, useMessage } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddOrEditCluePoolDrawer from './addOrEditCluePoolDrawer.vue';

  import { deleteLeadPool, getLeadPoolPage, switchLeadPoolStatus } from '@/api/modules/system/module';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { LeadPoolItem } from '@lib/shared/models/system/module';

  const { openModal } = useModal();
  const Message = useMessage();
  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const showAddOrEditDrawer = ref<boolean>(false);
  // 增加
  function handleAdd() {
    showAddOrEditDrawer.value = true;
  }

  // 编辑
  const currentRow = ref<LeadPoolItem>();
  async function handleEdit(row: LeadPoolItem) {
    currentRow.value = row;
    showAddOrEditDrawer.value = true;
  }

  const tableRefreshId = ref(0);

  // 删除
  function handleDelete(row: LeadPoolItem) {
    // TODO 判断是否存在未分配的线索
    const hasData = true;
    const title = hasData
      ? t('module.clue.deleteRulesTitle')
      : t('common.deleteConfirmTitle', { name: characterLimit(row.name) });
    const content = hasData ? '' : t('module.deleteTip', { name: t('module.clue') });
    const positiveText = t(hasData ? 'opportunity.gotIt' : 'common.confirm');
    const negativeText = t(hasData ? 'opportunity.goMove' : 'common.cancel');

    openModal({
      type: 'error',
      title,
      content,
      positiveText,
      negativeText,
      onPositiveClick: async () => {
        try {
          await deleteLeadPool(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: LeadPoolItem, actionKey: string) {
    switch (actionKey) {
      case 'pop-edit':
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
  async function handleToggleStatus(row: LeadPoolItem) {
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
          await switchLeadPoolStatus(row.id);
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
      width: 200,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
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
          value: 1,
        },
        {
          label: t('common.disable'),
          value: 0,
        },
      ],
      filter: true,
      render: (row: LeadPoolItem) => {
        return h(NSwitch, {
          value: row.enable,
          onClick: () => {
            handleToggleStatus(row);
          },
        });
      },
    },
    {
      title: t('opportunity.admin'),
      key: 'owners',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('role.member'),
      key: 'members',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('module.clue.autoRecycle'),
      key: 'auto',
      width: 100,
      sortOrder: false,
      sorter: true,
      filterOptions: [
        {
          label: t('common.yes'),
          value: 1,
        },
        {
          label: t('common.no'),
          value: 0,
        },
      ],
      filter: true,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 100,
      render: (row: LeadPoolItem) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 80,
      fixed: 'right',
      render: (row: LeadPoolItem) =>
        h(CrmOperationButton, {
          groupList: [
            {
              label: t('common.edit'),
              key: 'edit',
              popConfirmProps: {
                loading: false,
                title: t('common.updateConfirmTitle'),
                content: t('module.clue.updateConfirmContent'),
                positiveText: t('common.updateConfirm'),
              },
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

  const { propsRes, propsEvent, loadList } = useTable(getLeadPoolPage, {
    tableKey: TableKeyEnum.MODULE_CLUE_POOL,
    showSetting: true,
    columns,
    scrollX: 1600,
  });

  onBeforeMount(() => {
    loadList();
  });

  watch(
    () => tableRefreshId.value,
    () => {
      loadList();
    }
  );
</script>
