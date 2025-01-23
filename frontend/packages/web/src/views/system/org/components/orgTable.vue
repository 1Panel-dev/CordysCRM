<template>
  <div class="w-full p-[24px]">
    <div class="mb-[16px]">
      <n-button class="mr-[12px]" type="primary" @click="addMember">{{ t('org.addStaff') }}</n-button>
      <CrmMoreAction :options="moreActions" trigger="click">
        <n-button type="default" class="outline--secondary">
          {{ t('common.more') }}
          <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
        </n-button>
      </CrmMoreAction>
    </div>
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
    <AddMember v-model:show="showDrawer" />
    <SyncWeChat v-model:show="showSyncWeChatModal" />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn, CrmTableDataItem } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import AddMember from './addMember.vue';
  import SyncWeChat from './syncWeChat.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { CommonList } from '@lib/shared/models/common';

  const { t } = useI18n();

  const isHasSetting = ref<boolean>(false);

  const showSyncWeChatModal = ref<boolean>(false);
  function settingWeChat(e: MouseEvent) {
    e.stopPropagation();
    showSyncWeChatModal.value = true;
  }

  function renderSync() {
    return h(
      'div',
      {
        class: `flex items-center ${isHasSetting.value ? 'text-[var(--text-n1)]' : 'text-[var(--text-n6)]'}`,
      },
      [
        t('org.enterpriseWhatSync'),
        h(CrmIcon, {
          type: 'iconicon_set_up',
          size: 16,
          class: 'ml-2 text-[var(--primary-8)]',
          onClick: (e: MouseEvent) => settingWeChat(e),
        }),
      ]
    );
  }

  const moreActions: ActionsItem[] = [
    {
      label: t('org.enterpriseWhatSync'),
      key: 'sync',
      render: renderSync(),
    },
    {
      label: t('common.import'),
      key: 'import',
    },

    {
      label: t('common.export'),
      key: 'export',
    },
  ];

  // TODO 未调整样式
  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
    },
    {
      title: t('org.userName'),
      key: 'userName',
      width: 100,
      sortOrder: false,
      sorter: true,
      showInTable: true,
    },
    {
      title: t('common.status'),
      key: 'status',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      showInTable: true,
      filterOptions: [
        {
          label: '222',
          value: '222',
        },
        {
          label: 'string',
          value: 'string',
        },
      ],
      filter: true,
    },
    {
      title: t('org.gender'),
      key: 'gender',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      filterOptions: [
        {
          label: t('org.male'),
          value: 'male',
        },
        {
          label: t('org.female'),
          value: 'female',
        },
      ],
      filter: 'default',
      showInTable: true,
    },
    {
      title: t('org.phoneNumber'),
      key: 'phoneNumber',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
    },
    {
      title: t('org.userEmail'),
      key: 'userEmail',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: true,
    },
    {
      title: t('org.department'),
      key: 'department',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: true,
    },
    {
      title: t('org.directSuperior'),
      key: 'directSuperior',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: false,
    },
    {
      title: t('org.role'),
      key: 'role',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.employeeNumber'),
      key: 'employeeNumber',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.Position'),
      key: 'Position',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.employeeType'),
      key: 'employeeType',
      width: 80,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.workingCity'),
      key: 'workingCity',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.userGroup'),
      key: 'userGroup',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('common.creator'),
      key: 'creator',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUserName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    { key: 'operation', width: 80 },
  ];

  function initData() {
    const data: CommonList<CrmTableDataItem<any>> = {
      total: 11,
      pageSize: 10,
      current: 1,
      list: [
        {
          id: '11',
          num: 'string',
          title: 'string',
          status: 'string',
          updateTime: null,
          createTime: null,
        },
        {
          id: '22',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
        },
      ],
    };
    return new Promise<CommonList<CrmTableDataItem<any>>>((resolve) => {
      setTimeout(() => {
        resolve(data);
      }, 200);
    });
  }

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(initData, {
    tableKey: TableKeyEnum.SYSTEM_ORG_TABLE,
    showSetting: true,
    columns,
  });

  function initOrgList() {
    setLoadListParams({ keyword: '' });
    loadList();
  }

  const showDrawer = ref<boolean>(false);

  function addMember() {
    showDrawer.value = true;
  }

  onMounted(() => {
    initOrgList();
  });
</script>

<style scoped></style>
