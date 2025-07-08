<template>
  <div class="p-[24px]">
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    >
      <template #actionLeft>
        <n-button v-permission="['SYSTEM_ROLE:ADD_USER']" type="primary" @click="emit('create')">
          {{ t('role.addMember') }}
        </n-button>
      </template>
      <template #actionRight>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
      </template>
    </CrmTable>
  </div>
</template>

<script setup lang="ts">
  import { NButton, useMessage } from 'naive-ui';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';

  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    activeFolderId?: string | number;
  }>();
  const emit = defineEmits<{
    (e: 'create'): void;
    (e: 'edit', id: string | number): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const tableRefreshId = ref(0);
  async function removeDashboard(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('dashboard.deleteDashboardTip'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      positiveButtonProps: {
        type: 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        try {
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const groupList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      permission: [],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
      permission: [],
    },
  ];

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        emit('edit', row.id);
        break;
      case 'delete':
        removeDashboard(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn<any>[] = [
    {
      title: t('common.name'),
      key: 'name',
      width: 200,
      fixed: 'left',
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.desc'),
      key: 'description',
      width: 300,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('dashboard.folder'),
      key: 'folder',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('dashboard.members'),
      key: 'members',
      ellipsis: {
        tooltip: true,
      },
      isTag: true,
      width: 150,
    },
    {
      title: t('common.creator'),
      key: 'creatorName',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 200,
      sortOrder: false,
      sorter: true,
    },
    {
      key: 'operation',
      width: 170,
      fixed: 'right',
      render: (row: any) =>
        h(CrmOperationButton, {
          groupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    async () => Promise.resolve([]),
    {
      tableKey: TableKeyEnum.ROLE_MEMBER,
      showSetting: true,
      columns,
      permission: [],
    },
    (item) => {
      return {
        ...item,
      };
    }
  );

  const keyword = ref('');
  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, folderId: props.activeFolderId });
    loadList();
  }
</script>

<style lang="less" scoped></style>
