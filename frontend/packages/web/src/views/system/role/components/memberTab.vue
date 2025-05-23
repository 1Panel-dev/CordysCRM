<template>
  <div class="relative h-full">
    <n-scrollbar
      x-scrollable
      :content-style="{ 'min-width': '600px', 'width': '100%', 'padding': '0 24px 24px', 'height': '100%' }"
    >
      <CrmTable
        v-model:checked-row-keys="checkedRowKeys"
        v-bind="propsRes"
        :action-config="actionConfig"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @batch-action="handleBatchAction"
      >
        <template #actionLeft>
          <n-button v-permission="['SYSTEM_ROLE:ADD_USER']" type="primary" @click="handleCreate">
            {{ t('role.addMember') }}
          </n-button>
        </template>
        <template #actionRight>
          <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
        </template>
      </CrmTable>
    </n-scrollbar>
  </div>
  <CrmSelectUserDrawer
    v-model:visible="drawerVisible"
    :loading="addMemberLoading"
    :title="t('role.addMember')"
    :api-type-key="MemberApiTypeEnum.SYSTEM_ROLE"
    :base-params="{ roleId: props.activeRoleId }"
    @confirm="handleAddConfirm"
  />
</template>

<script setup lang="ts">
  import { NButton, NScrollbar, NSwitch, useMessage } from 'naive-ui';

  import { MemberApiTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { DeptUserTreeNode, RoleMemberItem } from '@lib/shared/models/system/role';

  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmRemoveButton from '@/components/pure/crm-remove-button/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmSelectUserDrawer from '@/components/business/crm-select-user-drawer/index.vue';

  import { batchRemoveRoleMember, getRoleMember, relateRoleMember, removeRoleMember } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    activeRoleId: string;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

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
      title: t('role.memberName'),
      key: 'userName',
      width: 200,
      fixed: 'left',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      columnSelectorDisabled: true,
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
          value: true,
        },
        {
          label: t('common.disable'),
          value: false,
        },
      ],
      filter: true,
      render: (row) => {
        return h(NSwitch, { value: row.enable, disabled: true });
      },
    },
    {
      title: t('role.department'),
      key: 'departmentId',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      sorter: true,
      sortOrder: false,
      render: (row: RoleMemberItem) => {
        return row.departmentName || '-';
      },
    },
    {
      title: t('role.job'),
      key: 'position',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      width: 200,
    },
    {
      title: t('role.role'),
      key: 'roles',
      width: 200,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('common.addTime'),
      key: 'createTime',
      width: 200,
      sortOrder: false,
      sorter: true,
    },
    {
      key: 'operation',
      minWidth: 100,
      fixed: 'right',
      render: (row) =>
        h(CrmRemoveButton, {
          loading: removeLoading.value,
          title: t('common.removeConfirmTitle', { name: characterLimit(row.userName) }),
          content: t('role.removeMemberTip'),
          permission: ['SYSTEM_ROLE:REMOVE_USER'],
          onConfirm: (cancel) => removeMember(row, cancel),
        }),
    },
  ];

  if (hasAnyPermission(['SYSTEM_ROLE:REMOVE_USER'])) {
    columns.unshift({
      type: 'selection',
      fixed: 'left',
    });
  }

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    getRoleMember,
    {
      tableKey: TableKeyEnum.ROLE_MEMBER,
      showSetting: true,
      columns,
      permission: ['SYSTEM_ROLE:REMOVE_USER'],
    },
    (item) => {
      return {
        ...item,
        position: item.position || '-',
        departmentName: item.departmentName || '-',
      };
    }
  );

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('role.batchRemove'),
        key: 'batchRemove',
        permission: ['SYSTEM_ROLE:REMOVE_USER'],
      },
    ],
  };

  const keyword = ref('');
  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, roleId: props.activeRoleId });
    loadList();
  }

  const checkedRowKeys = ref<(string | number)[]>([]);

  function batchRemoveMember() {
    openModal({
      type: 'warning',
      title: t('role.batchRemoveTip', { count: checkedRowKeys.value.length }),
      content: t('role.removeMemberTip'),
      positiveText: t('role.batchRemoveConfirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchRemoveRoleMember(checkedRowKeys.value);
          checkedRowKeys.value = [];
          searchData();
          Message.success(t('common.removeSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchRemove':
        batchRemoveMember();
        break;
      default:
        break;
    }
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

  const addMemberLoading = ref(false);
  async function handleAddConfirm(params: DeptUserTreeNode[], offspringNodes: DeptUserTreeNode[]) {
    try {
      addMemberLoading.value = true;
      const categorizedIds = params.concat(offspringNodes).reduce(
        (acc, item) => {
          switch (item.nodeType) {
            case DeptNodeTypeEnum.USER:
              acc.userIds.push(item.id);
              break;
            case DeptNodeTypeEnum.ROLE:
              acc.roleIds.push(item.id);
              break;
            case DeptNodeTypeEnum.ORG:
              acc.deptIds.push(item.id);
              break;
            default:
              break;
          }
          return acc;
        },
        {
          userIds: [] as string[],
          roleIds: [] as string[],
          deptIds: [] as string[],
        }
      );
      await relateRoleMember({
        ...categorizedIds,
        roleId: props.activeRoleId,
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
    searchData();
  });
</script>

<style lang="less"></style>
