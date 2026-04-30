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
    <addProcessDrawer
      v-model:visible="showProcessDrawer"
      :sourceId="activeSourceId"
      :is-detail="isDetail"
      @refresh="initData"
      @cancel="handleCancel"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { ApprovalProcessItem } from '@lib/shared/models/system/process';

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

  import {
    approvalProcessDetail,
    deleteApprovalProcess,
    getApprovalProcessList,
    toggleApprovalProcess,
    updateApprovalProcess,
  } from '@/api/modules';
  import { businessTypeOptions } from '@/config/process';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();

  const { t } = useI18n();
  const Message = useMessage();

  const keyword = ref('');
  const tableRefreshId = ref(0);
  const activeSourceId = ref();

  const showProcessDrawer = ref(false);
  const isDetail = ref(false);
  // 添加
  function handleAdd() {
    showProcessDrawer.value = true;
    isDetail.value = false;
  }

  function handleCancel() {
    showProcessDrawer.value = false;
    activeSourceId.value = '';
    isDetail.value = false;
  }

  async function deleteHandler(row: ApprovalProcessItem) {
    const enabled = row.enable;
    const content = enabled ? t('process.process.deleteEnabledContent') : t('process.process.deleteContent');
    const positiveText = enabled ? t('common.gotIt') : t('common.confirm');
    const type = enabled ? 'default' : 'error';
    openModal({
      type,
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content,
      positiveText,
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          if (!row.enable) {
            await deleteApprovalProcess(row.id);
            tableRefreshId.value += 1;
            Message.success(t('common.deleteSuccess'));
          }
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: ApprovalProcessItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        activeSourceId.value = row.id;
        showProcessDrawer.value = true;
        isDetail.value = false;
        break;
      case 'delete':
        deleteHandler(row);
        break;
      default:
        break;
    }
  }

  async function handleToggleStatus(row: ApprovalProcessItem) {
    try {
      await toggleApprovalProcess(row.id, !row.enable);
      Message.success(t(!row.enable ? 'common.enableSuccess' : 'common.closeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleChangeName(id: string, name: string) {
    try {
      const result = await approvalProcessDetail(id);
      await updateApprovalProcess({
        ...result,
        name,
      });
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
      key: 'number',
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
      key: 'formType',
      width: 200,
      render: (row: ApprovalProcessItem) =>
        h(CrmNameTooltip, { text: businessTypeOptions.find((e) => e.value === row.formType)?.label ?? '-' }),
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
      render: (row: ApprovalProcessItem) => {
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
                  class: ' w-[fit-content]',
                },
                h(
                  CrmTableButton,
                  {
                    onClick: () => {
                      activeSourceId.value = row.id;
                      showProcessDrawer.value = true;
                      isDetail.value = true;
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
      key: 'enable',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      filter: true,
      filterOptions: [
        {
          value: true,
          label: t('common.enable'),
        },
        {
          value: false,
          label: t('common.disable'),
        },
      ],
      width: 200,
      render: (row: ApprovalProcessItem) =>
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
      key: 'executeTiming',
      width: 200,
      render: (row: ApprovalProcessItem) => {
        return h(CrmNameTooltip, {
          text: row.executeTiming.includes('CREATE') ? t('common.create') : t('common.edit'),
        });
      },
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      sortOrder: false,
      sorter: true,
      width: 200,
      render: (row: ApprovalProcessItem) => {
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
      render: (row: ApprovalProcessItem) => {
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
      render: (row: ApprovalProcessItem) =>
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

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getApprovalProcessList, {
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

  function searchData(val?: string) {
    keyword.value = val ?? keyword.value;
    initData();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      searchData();
    }
  );

  onBeforeMount(() => {
    initData();
  });
</script>

<style scoped></style>
