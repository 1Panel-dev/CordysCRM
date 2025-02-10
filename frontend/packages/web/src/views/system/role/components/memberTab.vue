<template>
  <div class="relative h-full">
    <n-scrollbar x-scrollable :content-style="{ 'min-width': '600px', 'width': '100%', 'padding': '0 24px' }">
      <div class="mb-[16px] mt-[4px] flex items-center justify-between">
        <n-button type="primary" @click="handleCreate">
          {{ t('role.addMember') }}
        </n-button>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
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
    :loading="addMemberLoading"
    @cancel="handleCancelAdd"
    @confirm="handleAddConfirm"
  >
    <div class="flex h-full w-full flex-col gap-[16px]">
      <div class="flex items-center gap-[16px]">
        <div class="whitespace-nowrap">{{ t('role.addMemberType') }}</div>
        <n-tabs
          v-model:value="addMemberType"
          type="segment"
          class="no-content"
          animated
          @update-value="handleTypeChange"
        >
          <n-tab-pane v-for="item of addMemberTypes" :key="item.value" :name="item.value" :tab="item.label">
          </n-tab-pane>
        </n-tabs>
      </div>
      <n-transfer
        v-model:value="addMembers"
        :options="flattenTree(options as unknown as Option[])"
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
    NScrollbar,
    NSwitch,
    NTabPane,
    NTabs,
    NTransfer,
    NTree,
    TransferRenderSourceList,
    useMessage,
  } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmRemoveButton from '@/components/pure/crm-remove-button/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import roleTreeNodePrefix from './roleTreeNodePrefix.vue';

  import {
    getRoleDeptUserTree,
    getRoleMember,
    getRoleMemberTree,
    getUsers,
    relateRoleMember,
    removeRoleMember,
  } from '@/api/modules/system/role';
  import { useI18n } from '@/hooks/useI18n';
  import { mapTree } from '@/utils';

  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { DeptTreeNode, RoleItem, RoleMemberItem } from '@lib/shared/models/system/role';

  interface Option {
    label: string;
    value: string;
    children?: Option[];
  }

  const props = defineProps<{
    activeRoleId: string;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const tableRefreshId = ref(0);
  const removeLoading = ref(false);
  async function removeMember(row: RoleMemberItem, close: () => void) {
    try {
      removeLoading.value = true;
      await removeRoleMember(row.id);
      Message.success(t('common.removeSuccess'));
      close();
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      removeLoading.value = false;
    }
  }

  const columns: CrmDataTableColumn<RoleMemberItem>[] = [
    {
      type: 'selection',
      fixed: 'left',
    },
    {
      title: t('role.memberName'),
      key: 'userName',
      width: 100,
      fixed: 'left',
      ellipsis: {
        tooltip: true,
      },
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
          label: t('common.enable'),
          value: 'enable',
        },
        {
          label: t('common.disable'),
          value: 'disabled',
        },
      ],
      filter: true,
      render: (row) => {
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
      sorter: true,
      sortOrder: false,
    },
    {
      title: t('role.job'),
      key: 'position',
      sortOrder: false,
      sorter: true,
      width: 100,
    },
    {
      title: t('role.role'),
      key: 'roles',
      width: 100,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('common.addTime'),
      key: 'createTime',
      width: 160,
    },
    {
      key: 'operation',
      width: 80,
      fixed: 'right',
      render: (row) =>
        h(CrmRemoveButton, {
          loading: removeLoading.value,
          title: t('common.removeConfirmTitle', { name: row.userName }),
          content: t('role.removeMemberTip'),
          onConfirm: (cancel) => removeMember(row, cancel),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    getRoleMember,
    {
      tableKey: TableKeyEnum.SYSTEM_USER,
      showSetting: true,
      columns,
      scrollX: 1000,
      maxHeight: 600,
    },
    (item) => {
      return {
        ...item,
        position: item.position || '-',
        departmentName: item.departmentName || '-',
      };
    }
  );

  const keyword = ref('');
  function searchData() {
    setLoadListParams({ keyword: keyword.value, roleId: props.activeRoleId });
    loadList();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      searchData();
    }
  );

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
  const userIds = ref<string[]>([]);
  const roleIds = ref<string[]>([]);
  const deptIds = ref<string[]>([]);
  const departmentOptions = ref<DeptTreeNode[]>([]);

  async function initDeptUserTree() {
    try {
      departmentOptions.value = await getRoleDeptUserTree(props.activeRoleId);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const roleOptions = ref<Option[]>([]);
  async function initRoleUserTree() {
    try {
      roleOptions.value = await getRoleMemberTree(props.activeRoleId);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const userOptions = ref<RoleItem[]>([]);
  async function initUserList() {
    try {
      userOptions.value = await getUsers(props.activeRoleId);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleTypeChange(value: string) {
    if (value === 'org') {
      initDeptUserTree();
    } else if (value === 'role') {
      initRoleUserTree();
    } else {
      initUserList();
    }
    addMembers.value = [];
  }

  const options = computed(() => {
    if (addMemberType.value === 'org') {
      return mapTree(departmentOptions.value, (item) => {
        return {
          label: item.name,
          value: item.id,
          disabled: !item.enabled,
          children: item.children?.length ? item.children : undefined,
          nodeType: item.nodeType,
        };
      });
    }
    if (addMemberType.value === 'role') {
      return mapTree(roleOptions.value, (item) => {
        return {
          label: item.name,
          value: item.id,
          disabled: !item.enabled,
          children: item.children?.length ? item.children : undefined,
          nodeType: item.nodeType,
        };
      });
    }
    return userOptions.value.map((item) => {
      return {
        label: item.name,
        value: item.id,
        disabled: !item.enabled,
        nodeType: item.nodeType,
      };
    });
  });

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
      renderPrefix(node: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
        if (node.option.internal) {
          return h(roleTreeNodePrefix);
        }
      },
      onUpdateSelectedKeys: (selectedKeys: Array<string | number>, nodes) => {
        onCheck(selectedKeys);
        userIds.value = [];
        roleIds.value = [];
        deptIds.value = [];
        nodes.forEach((node) => {
          if (node) {
            if (node.nodeType === DeptNodeTypeEnum.USER) {
              userIds.value.push(node.value as string);
            } else if (node.nodeType === DeptNodeTypeEnum.ROLE) {
              roleIds.value.push(node.value as string);
            } else if (node.nodeType === DeptNodeTypeEnum.ORG) {
              deptIds.value.push(node.value as string);
            }
          }
        });
      },
    });
  };

  function handleCancelAdd() {
    addMembers.value = [];
    userIds.value = [];
    roleIds.value = [];
    deptIds.value = [];
    addMemberType.value = 'org';
  }

  const addMemberLoading = ref(false);
  async function handleAddConfirm() {
    try {
      addMemberLoading.value = true;
      await relateRoleMember({
        roleId: props.activeRoleId,
        userIds: userIds.value,
        roleIds: roleIds.value,
        deptIds: deptIds.value,
      });
      Message.success(t('common.addSuccess'));
      drawerVisible.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      addMemberLoading.value = false;
      searchData();
    }
  }

  watch(
    () => props.activeRoleId,
    () => {
      searchData();
    }
  );

  onMounted(() => {
    initDeptUserTree();
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
