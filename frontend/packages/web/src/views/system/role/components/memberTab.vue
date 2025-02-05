<template>
  <div class="relative h-full">
    <n-scrollbar x-scrollable :content-style="{ 'min-width': '600px', 'width': '100%', 'padding': '0 24px' }">
      <div class="mb-[16px] mt-[4px] flex items-center justify-between">
        <n-button type="primary" @click="handleCreate">
          {{ t('role.addMember') }}
        </n-button>
        <n-input v-model:value="keyword" :placeholder="t('common.searchByName')" clearable class="!w-[240px]">
          <template #suffix>
            <n-icon>
              <Search />
            </n-icon>
          </template>
        </n-input>
      </div>
      <crm-table
        v-bind="propsRes"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
      />
    </n-scrollbar>
  </div>
  <CrmDrawer
    v-model:show="drawerVisible"
    :title="t('role.addMember')"
    :width="800"
    :ok-disabled="addMembers.length === 0"
  >
    <div class="flex h-full w-full flex-col gap-[16px]">
      <div class="flex items-center gap-[16px]">
        <div class="whitespace-nowrap">{{ t('role.addMemberType') }}</div>
        <n-tabs
          v-model:value="addMemberType"
          type="segment"
          class="no-content"
          animated
          @update-value="
            () => {
              addMembers = [];
            }
          "
        >
          <n-tab-pane v-for="item of addMemberTypes" :key="item.value" :name="item.value" :tab="item.label">
          </n-tab-pane>
        </n-tabs>
      </div>
      <n-transfer
        v-model:value="addMembers"
        :options="flattenTree(options)"
        :render-source-list="renderSourceList"
        source-filterable
        class="addMemberTransfer"
      />
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import {
    NButton,
    NIcon,
    NInput,
    NScrollbar,
    NSwitch,
    NTabPane,
    NTabs,
    NTransfer,
    NTree,
    TransferRenderSourceList,
    useMessage,
  } from 'naive-ui';
  import { Search } from '@vicons/ionicons5';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmRemoveButton from '@/components/pure/crm-remove-button/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn, CrmTableDataItem } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import { useI18n } from '@/hooks/useI18n';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { CommonList } from '@lib/shared/models/common';

  interface RoleItem {
    id: string;
    name: string;
  }

  interface MemberItem {
    id: string;
    num: string;
    userName: string;
    enable: boolean;
    departmentName: string;
    position: string;
    createTime: number;
    roles: RoleItem[];
  }

  const { t } = useI18n();
  const Message = useMessage();

  const removeLoading = ref(false);
  async function removeMember(row: Record<string, any>, close: () => void) {
    try {
      removeLoading.value = true;
      Message.success(t('common.removeSuccess'));
      close();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      removeLoading.value = false;
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
      fixed: 'left',
    },
    {
      title: t('role.memberName'),
      key: 'userName',
      width: 100,
      fixed: 'left',
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
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
      render: (row: Record<string, any>) => {
        return h(NSwitch, { value: row.enable, disabled: true });
      },
    },
    {
      title: t('role.department'),
      key: 'departmentName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      filterOptions: [
        {
          label: 'London',
          value: 'London',
        },
        {
          label: 'New York',
          value: 'New York',
        },
      ],
      filter: 'default',
    },
    {
      title: t('role.job'),
      key: 'position',
      width: 100,
    },
    {
      title: t('role.role'),
      key: 'roles',
      width: 100,
    },
    {
      title: t('common.addTime'),
      key: 'createTime',
      width: 180,
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 180,
    },
    {
      key: 'operation',
      width: 80,
      fixed: 'right',
      render: (row: Record<string, any>) =>
        h(CrmRemoveButton, {
          loading: removeLoading.value,
          title: t('common.removeConfirmTitle', { name: row.userName }),
          content: t('role.removeMemberTip'),
          onConfirm: (cancel) => removeMember(row, cancel),
        }),
    },
  ];

  function getRoleList() {
    const data: CommonList<CrmTableDataItem<MemberItem>> = {
      list: [
        {
          id: '11',
          num: 'string',
          userName: 'string',
          enable: true,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          updateTime: 1738722457410,
          createTime: 1738722457410,
        },
        {
          id: '22',
          num: '232324323',
          userName: '222',
          enable: true,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          createTime: 1738722457410,
        },
        {
          id: '33',
          num: 'string',
          userName: 'string',
          enable: false,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          createTime: 1738722457410,
        },
        {
          id: '44',
          num: '232324323',
          userName: '222',
          enable: false,
          updateTime: 1738722457410,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          createTime: 1738722457410,
        },
        {
          id: '55',
          num: 'string',
          userName: 'string',
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          enable: false,
          updateTime: 1738722457410,
          createTime: 1738722457410,
        },
        {
          id: '66',
          num: '232324323',
          userName: '222',
          enable: false,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          updateTime: 1738722457410,
          createTime: 1738722457410,
        },
        {
          id: '77',
          num: 'string',
          userName: 'string',
          enable: false,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          updateTime: 1738722457410,
          createTime: 1738722457410,
        },
        {
          id: '88',
          num: '232324323',
          userName: '222',
          enable: false,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          updateTime: 1738722457410,
          createTime: 1738722457410,
        },
        {
          id: '99',
          num: 'string',
          userName: 'string',
          enable: true,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          updateTime: 1738722457410,
          createTime: 1738722457410,
        },
        {
          id: '1',
          num: '232324323',
          userName: '222',
          enable: true,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          updateTime: 1738722457410,
          createTime: 1738722457410,
        },
        {
          id: '2',
          num: '232324323',
          userName: '222',
          enable: true,
          departmentName: 'bumen',
          position: 'job',
          roles: [
            {
              id: '1',
              name: 'role1',
            },
            {
              id: '2',
              name: 'role2',
            },
          ],
          updateTime: 1738722457410,
          createTime: 1738722457410,
        },
      ],
      total: 11,
      pageSize: 10,
      current: 1,
    };
    return new Promise<CommonList<CrmTableDataItem<MemberItem>>>((resolve) => {
      setTimeout(() => {
        resolve(data);
      }, 200);
    });
  }

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getRoleList, {
    tableKey: TableKeyEnum.SYSTEM_USER,
    showSetting: true,
    columns,
    scrollX: 1000,
    maxHeight: 600,
  });

  const keyword = ref('');
  function searchData() {
    setLoadListParams({ keyword: keyword.value });
    loadList();
  }

  const drawerVisible = ref(false);
  function handleCreate() {
    drawerVisible.value = true;
  }
  const addMemberType = ref<string>('org');
  const addMemberTypes = [
    {
      label: t('menu.settings.org'),
      value: 'org',
    },
    {
      label: t('role.role'),
      value: 'role',
    },
    {
      label: t('role.member'),
      value: 'member',
    },
  ];
  const addMembers = ref<string[]>([]);
  const options = ref([
    {
      label: '一级 1',
      value: '1',
      children: [
        {
          label: '二级 1-1',
          value: '1-1',
          children: [
            {
              label: '三级 1-1-1',
              value: '1-1-1',
            },
            {
              label: '三级 1-1-2',
              value: '1-1-2',
            },
            {
              label: '三级 1-1-3',
              value: '1-1-3',
            },
          ],
        },
      ],
    },
    {
      label: '一级 2',
      value: '2222',
      children: [
        {
          label: '二级 2-1',
          value: '2-1',
          children: [
            {
              label: '三级 2-1-1',
              value: '2-1-1',
            },
          ],
        },
      ],
    },
  ]);

  interface Option {
    label: string;
    value: string;
    children?: Option[];
  }

  function flattenTree(list: undefined | Option[]): Option[] {
    const result: Option[] = [];
    function flatten(_list: Option[] = []) {
      _list.forEach((item) => {
        result.push(item);
        flatten(item.children);
      });
    }
    flatten(list);
    return result;
  }

  const renderSourceList: TransferRenderSourceList = ({ onCheck, pattern }) => {
    return h(NTree, {
      keyField: 'value',
      blockLine: true,
      multiple: true,
      selectable: true,
      data: options.value,
      pattern,
      selectedKeys: addMembers.value,
      showIrrelevantNodes: false,
      onUpdateSelectedKeys: (selectedKeys: Array<string | number>) => {
        onCheck(selectedKeys);
      },
    });
  };

  onMounted(() => {
    searchData();
  });
</script>

<style lang="less">
  .addMemberTransfer {
    @apply flex-1;
    .n-transfer-list {
      @apply h-full;
    }
  }
</style>
