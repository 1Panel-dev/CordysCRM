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

  import CrmRemoveButton from '@/components/pure/crm-remove-button/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmSelectUserDrawer from '@/components/business/crm-select-user-drawer/index.vue';
  import { SelectedUsersParams } from '@/components/business/crm-select-user-drawer/type';

  import { getRoleMember, relateRoleMember, removeRoleMember } from '@/api/modules/system/role';
  import { useI18n } from '@/hooks/useI18n';

  import { MemberApiTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { RoleMemberItem } from '@lib/shared/models/system/role';

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

  const addMemberLoading = ref(false);
  async function handleAddConfirm(params: SelectedUsersParams) {
    try {
      addMemberLoading.value = true;
      await relateRoleMember({
        ...params,
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

<style lang="less">
  .addMemberTransfer {
    @apply flex-1;
    .n-transfer-list {
      @apply h-full;
    }
  }
</style>
