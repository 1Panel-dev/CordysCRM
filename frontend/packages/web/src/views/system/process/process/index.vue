<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <CrmTable
      ref="crmTableRef"
      v-bind="propsRes"
      class="crm-process-list-table"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @refresh="initData"
    >
      <template #tableTop>
        <div class="flex items-center justify-between">
          <n-button v-permission="['APPROVAL_FLOW:ADD']" type="primary" @click="handleAdd">
            {{ t('process.process.newProcess') }}
          </n-button>
          <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
        </div>
      </template>
    </CrmTable>
    <addProcessDrawer v-model:visible="showProcessDrawer" :sourceId="activeSourceId" />
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { AnnouncementItemDetail } from '@lib/shared/models/system/message';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import addProcessDrawer from './components/addProcessDrawer.vue';

  import { getAnnouncementList } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();

  const { t } = useI18n();
  const Message = useMessage();

  const keyword = ref('');
  const tableRefreshId = ref(0);
  const activeSourceId = ref();

  const showProcessDrawer = ref(false);

  // 添加
  function handleAdd() {
    showProcessDrawer.value = true;
  }

  async function deleteHandler(row: any, done?: () => void) {
    const enabled = row.enable;
    const content = enabled ? t('process.process.deleteEnabledContent') : t('process.process.deleteContent');
    const positiveText = enabled ? t('common.gotIt') : t('common.confirm');
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content,
      positiveText,
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        if (!enabled) {
          deleteHandler(row, done);
        }
        try {
          // todo
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        activeSourceId.value = row.id;
        showProcessDrawer.value = true;
        break;
      case 'delete':
        deleteHandler(row);
        break;
      default:
        break;
    }
  }

  async function handleToggleStatus(row: any) {
    const enable = !row.enable;
    try {
      // todo
      tableRefreshId.value += 1;
      Message.success(t(enable ? 'common.enableSuccess' : 'common.closeSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleChangeName(id: string, name: string) {
    try {
      // todo
      Message.success(t('common.updateSuccess'));
      return Promise.resolve(true);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
      return Promise.resolve(false);
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('crmTable.order'),
      width: 50,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      fixed: 'left',
      columnSelectorDisabled: true,
      render: (row: any, rowIndex: number) => rowIndex + 1,
    },
    {
      title: 'ID',
      key: 'id',
      width: 200,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      fixed: 'left',
      columnSelectorDisabled: true,
    },
    {
      title: t('process.process.processType'),
      key: 'type',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('process.process.name'),
      key: 'name',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
      render: (row: any) => {
        return h(
          CrmEditableText,
          {
            value: row.name ?? '',
            permission: ['APPROVAL_FLOW:UPDATE'],
            onHandleEdit: async (val: string, done?: () => void) => {
              const res = await handleChangeName(row.id, val);
              if (res) {
                done?.();
                tableRefreshId.value += 1;
              }
            },
          },
          {
            default: () =>
              h(
                'div',
                {
                  class: 'max-w-[calc(100%-24px)] w-[fit-content]',
                },
                h(
                  CrmTableButton,
                  {
                    onClick: () => {
                      activeSourceId.value = row.id;
                      showProcessDrawer.value = true;
                    },
                  },
                  { default: () => row.name, trigger: () => row.name }
                )
              ),
          }
        );
      },
    },
    {
      title: t('common.status'),
      key: 'status',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      filter: true,
      filterOptions: [
        {
          value: '1',
          label: t('common.enable'),
        },
        {
          value: '0',
          label: t('common.disable'),
        },
      ],
      width: 200,
      render: (row: any) =>
        h(
          NTooltip,
          {
            delay: 300,
          },
          {
            trigger: () => {
              return h(NSwitch, {
                size: 'small',
                rubberBand: false,
                value: row.enable,
                disabled: !hasAnyPermission(['APPROVAL_FLOW:UPDATE']),
                onClick: () => {
                  if (!hasAnyPermission(['APPROVAL_FLOW:UPDATE'])) return;
                  handleToggleStatus(row);
                },
              });
            },
            default: () => t('process.process.enableProcessTip'),
          }
        ),
    },
    {
      title: t('process.executionTiming'),
      key: 'executionTiming',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      sortOrder: false,
      sorter: true,
      width: 200,
      render: (row: any) => {
        return h(CrmNameTooltip, { text: row.createUserName });
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 200,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 200,
      sortOrder: false,
      sorter: true,
      render: (row: AnnouncementItemDetail) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      key: 'operation',
      width: 110,
      fixed: 'right',
      render: (row: AnnouncementItemDetail) =>
        h(CrmOperationButton, {
          groupList: [
            {
              label: t('common.edit'),
              key: 'edit',
              permission: ['APPROVAL_FLOW:UPDATE'],
            },
            {
              label: t('common.delete'),
              key: 'delete',
              permission: ['APPROVAL_FLOW:DELETE'],
            },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];
  // todo xinxinwu
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getAnnouncementList, {
    tableKey: TableKeyEnum.PROCESS,
    showSetting: true,
    columns,
    containerClass: '.crm-process-list-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  function initData() {
    setLoadListParams({
      keyword: keyword.value,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(val: string) {
    keyword.value = val;
    initData();
  }

  onBeforeMount(() => {
    initData();
  });
</script>

<style scoped></style>
