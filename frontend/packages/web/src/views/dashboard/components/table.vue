<template>
  <div class="h-full p-[24px]">
    <CrmTable
      ref="crmTableRef"
      v-bind="currentTable.propsRes.value"
      class="flex-1"
      @page-change="currentTable.propsEvent.value.pageChange"
      @page-size-change="currentTable.propsEvent.value.pageSizeChange"
      @sorter-change="currentTable.propsEvent.value.sorterChange"
      @filter-change="currentTable.propsEvent.value.filterChange"
    >
      <template #tableTop>
        <div class="flex items-center justify-between">
          <n-button v-permission="['DASHBOARD:ADD']" type="primary" @click="emit('create')">
            {{ t('dashboard.addDashboard') }}
          </n-button>
          <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
        </div>
      </template>
    </CrmTable>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { NButton, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';

  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import favoriteIcon from './favoriteIcon.vue';

  import {
    dashboardCollect,
    dashboardCollectPage,
    dashboardDelete,
    dashboardPage,
    dashboardUnCollect,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import { FullPageEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    isFavorite?: boolean;
    activeFolderId?: string;
    offspringIds?: Array<string>;
  }>();
  const emit = defineEmits<{
    (e: 'create'): void;
    (e: 'edit', id: string): void;
    (e: 'collect', id: string, collect: boolean): void;
    (e: 'delete'): void;
  }>();

  const router = useRouter();
  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
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
          await dashboardDelete(row.id);
          Message.success(t('common.deleteSuccess'));
          emit('delete');
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
      permission: ['DASHBOARD:EDIT'],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: ['DASHBOARD:DELETE'],
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

  async function favoriteToggle(option: CrmTreeNodeData) {
    try {
      if (option.myCollect) {
        await dashboardUnCollect(option.id);
      } else {
        await dashboardCollect(option.id);
      }
      option.myCollect = !option.myCollect;
      Message.success(option.myCollect ? t('dashboard.favoriteSuccess') : t('dashboard.unFavoriteSuccess'));
      emit('collect', option.id, option.myCollect);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
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
      render: (row: any) => {
        return h(
          'div',
          {
            class: 'flex items-center',
          },
          {
            default: () => [
              h(favoriteIcon, {
                value: row.myCollect,
                class: 'mr-[8px] cursor-pointer text-[var(--primary-8)]',
                onclick: () => {
                  favoriteToggle(row);
                },
              }),
              h(
                CrmTableButton,
                {
                  onClick: () => {
                    window.open(
                      `${window.location.origin}#${
                        router.resolve({ name: FullPageEnum.FULL_PAGE_DASHBOARD }).fullPath
                      }?id=${row.resourceId}&isFavorite=${row.myCollect ? 'Y' : 'N'}&title=${encodeURIComponent(
                        row.name
                      )}`,
                      '_blank'
                    );
                  },
                },
                { trigger: () => row.name, default: () => row.name }
              ),
            ],
          }
        );
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
      key: 'dashboardModuleName',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('dashboard.members'),
      key: 'members',
      isTag: true,
      width: 150,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('common.creator'),
      key: 'createUserName',
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
      width: 100,
      fixed: 'right',
      render: (row: any) =>
        h(CrmOperationButton, {
          groupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const dashboardTable = useTable(dashboardPage, {
    showSetting: false,
    columns,
    permission: [],
  });

  const dashboardCollectTable = useTable(
    dashboardCollectPage,
    {
      showSetting: false,
      columns,
      permission: [],
    },
    (item) => {
      return {
        ...item,
      };
    }
  );

  // 当前展示的表格数据类型
  const currentTable = computed(() => {
    if (props.isFavorite) {
      return dashboardCollectTable;
    }
    return dashboardTable;
  });

  const keyword = ref('');
  function searchData(val?: string) {
    currentTable.value.setLoadListParams({
      keyword: val ?? keyword.value,
      dashboardModuleIds: Array.from(new Set([props.activeFolderId, ...(props.offspringIds || [])])).filter(
        (item) => item && !['all', 'favorite'].includes(item)
      ),
    });
    currentTable.value.loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  watch(
    () => props.activeFolderId,
    () => {
      searchData();
    },
    {
      immediate: true,
    }
  );

  watch(
    () => tableRefreshId.value,
    () => {
      searchData();
    }
  );

  defineExpose({
    loadList: () => {
      searchData();
    },
  });
</script>

<style lang="less" scoped></style>
