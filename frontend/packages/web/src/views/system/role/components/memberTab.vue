<template>
  <div class="relative h-full">
    <n-scrollbar x-scrollable :content-style="{ 'min-width': '600px', 'width': '100%', 'padding': '0 24px' }">
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
          <n-button type="primary" @click="handleCreate">
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

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';
  import { RoleMemberItem } from '@lib/shared/models/system/role';

  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmRemoveButton from '@/components/pure/crm-remove-button/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmSelectUserDrawer from '@/components/business/crm-select-user-drawer/index.vue';

  import { batchRemoveRoleMember, getRoleMember, relateRoleMember, removeRoleMember } from '@/api/modules/system/role';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

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
      tableKey: TableKeyEnum.ROLE_MEMBER,
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

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('role.batchRemove'),
        key: 'batchRemove',
      },
    ],
  };

  const keyword = ref('');
  function searchData() {
    setLoadListParams({ keyword: keyword.value, roleId: props.activeRoleId });
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
  async function handleAddConfirm(params: SelectedUsersItem[]) {
    try {
      addMemberLoading.value = true;
      const categorizedIds = params.reduce(
        (acc, item) => {
          switch (item.scope) {
            case MemberSelectTypeEnum.MEMBER:
              acc.userIds.push(item.id);
              break;
            case MemberSelectTypeEnum.ROLE:
              acc.roleIds.push(item.id);
              break;
            case MemberSelectTypeEnum.ORG:
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
