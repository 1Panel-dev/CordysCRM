<template>
  <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[12px]">
    <div class="h-full rounded-[var(--border-radius-small)] bg-[var(--text-n10)] p-[24px]">
      <CrmMemberSelectorBox
        v-model:value="adminIds"
        v-model:selected-list="adminList"
        :label="t('customForm.formAdmin')"
        :add-text="t('org.addMember')"
        required
        :max-count="adminMaxCount"
        :tip-text="t('customForm.maxAddAdminTip', { count: adminMaxCount })"
        :clear-text="t('customForm.restoreDefault')"
        :preserve-value-on-clear="true"
        :api-type-key="MemberApiTypeEnum.FORM_FIELD"
        :member-types="userMemberTypes"
        :disabled-node-types="[DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]"
        @confirm="handleSaveAdmins"
        @delete-tag="handleSaveAdmins"
        @clear="handleRestoreDefaultAdmins"
      />

      <div class="my-[16px]">
        <div class="mb-[8px] flex items-center gap-[6px] font-semibold">
          {{ t('customForm.formMember') }}
          <n-tooltip trigger="hover" placement="right" :style="{ maxWidth: '500px' }">
            <template #trigger>
              <CrmIcon
                type="iconicon_help_circle"
                :size="16"
                class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
              />
            </template>
            <div>{{ t('customForm.permission.manageAllTip') }}</div>
            <div>{{ t('customForm.permission.viewAllTip') }}</div>
            <div>{{ t('customForm.permission.addManageOwnTip') }}</div>
          </n-tooltip>
        </div>
        <n-radio-group v-model:value="activePermissionType" name="customFormPermissionType">
          <n-radio-button v-for="item in permissionTabList" :key="item.name" :value="item.name" :label="item.tab" />
        </n-radio-group>
      </div>

      <CrmTable
        ref="crmTableRef"
        v-model:checked-row-keys="checkedRowKeys"
        v-bind="propsRes"
        class="custom-form-member-table"
        :action-config="actionConfig"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @batch-action="handleBatchAction"
        @refresh="searchData"
      >
        <template #actionLeft>
          <n-button type="primary" @click="handleCreate">
            {{ t('org.addMember') }}
          </n-button>
        </template>
      </CrmTable>
    </div>
  </div>

  <CrmSelectUserDrawer
    v-model:visible="memberDrawerVisible"
    :title="t('org.addMember')"
    :loading="addMemberLoading"
    :api-type-key="MemberApiTypeEnum.FORM_FIELD"
    @confirm="handleAddMembers"
  />
</template>

<script setup lang="ts">
  import { computed, h, onMounted, ref, watch } from 'vue';
  import { NButton, NRadioButton, NRadioGroup, NTooltip, useMessage } from 'naive-ui';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
  import { CustomFormDataPermissionTypeEnum, type CustomFormMemberItem } from '@lib/shared/models/customForm';
  import type { SelectedUsersItem } from '@lib/shared/models/system/module';
  import type { DeptUserTreeNode } from '@lib/shared/models/system/role';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmRemoveButton from '@/components/pure/crm-remove-button/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmMemberSelectorBox from '@/components/business/crm-member-selector-box/index.vue';
  import CrmSelectUserDrawer from '@/components/business/crm-select-user-drawer/index.vue';

  import {
    batchRemoveCustomFormMember,
    getCustomFormAdmins,
    getCustomFormMember,
    relateCustomFormMember,
    removeCustomFormMember,
    saveCustomFormAdmins,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const userMemberTypes = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
  ];

  const adminMaxCount = 15;
  const adminIds = ref<string[]>([]);
  const adminList = ref<SelectedUsersItem[]>([]);
  const defaultAdminList = ref<SelectedUsersItem[]>([]);
  const isAdminLimitExceeded = computed(() => adminList.value.length > adminMaxCount);

  async function loadAdmins() {
    try {
      adminList.value = await getCustomFormAdmins(props.sourceId);
      adminIds.value = adminList.value.map((item) => item.id);
      defaultAdminList.value = adminList.value.map((item) => ({ ...item }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleSaveAdmins() {
    if (isAdminLimitExceeded.value) {
      Message.warning(t('customForm.maxAddAdminTip', { count: adminMaxCount }));
      return;
    }

    try {
      await saveCustomFormAdmins({
        customFormId: props.sourceId,
        userIds: adminIds.value,
      });
      Message.success(t('common.saveSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleRestoreDefaultAdmins() {
    adminList.value = defaultAdminList.value.map((item) => ({ ...item }));
    adminIds.value = adminList.value.map((item) => item.id);
    handleSaveAdmins();
  }

  const permissionTabList = [
    {
      name: CustomFormDataPermissionTypeEnum.MANAGE_ALL,
      tab: t('customForm.permission.manageAll'),
    },
    {
      name: CustomFormDataPermissionTypeEnum.VIEW_ALL,
      tab: t('customForm.permission.viewAll'),
    },
    {
      name: CustomFormDataPermissionTypeEnum.ADD_MANAGE_OWN,
      tab: t('customForm.permission.addManageOwn'),
    },
  ];

  const activePermissionType = ref(CustomFormDataPermissionTypeEnum.MANAGE_ALL);

  const tableRefreshId = ref(0);
  const removeLoading = ref(false);

  async function removeMember(row: CustomFormMemberItem, close: () => void) {
    try {
      removeLoading.value = true;
      await removeCustomFormMember(row.id);
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

  const columns: CrmDataTableColumn<CustomFormMemberItem>[] = [
    {
      type: 'selection',
      fixed: 'left',
    },
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
      title: t('role.department'),
      key: 'departmentId',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => row.departmentName || '-',
    },
    {
      title: t('role.job'),
      key: 'position',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
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
      width: 100,
      fixed: 'right',
      render: (row) =>
        h(CrmRemoveButton, {
          loading: removeLoading.value,
          title: t('common.removeConfirmTitle', { name: characterLimit(row.userName) }),
          content: t('role.removeMemberTip'),
          onConfirm: (cancel) => removeMember(row, cancel),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    getCustomFormMember,
    {
      columns,
      showSetting: false,
      containerClass: '.custom-form-member-table',
    },
    (item) => ({
      ...item,
      position: item.position || '-',
      departmentName: item.departmentName || '-',
    })
  );

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('role.batchRemove'),
        key: 'batchRemove',
      },
    ],
  };

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  function searchData() {
    setLoadListParams({
      customFormId: props.sourceId,
      customFormRole: activePermissionType.value,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
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
          await batchRemoveCustomFormMember(checkedRowKeys.value);
          checkedRowKeys.value = [];
          crmTableRef.value?.clearCheckedRowKeys();
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
    if (item.key === 'batchRemove') {
      batchRemoveMember();
    }
  }

  const memberDrawerVisible = ref(false);
  const addMemberLoading = ref(false);

  function handleCreate() {
    memberDrawerVisible.value = true;
  }

  async function handleAddMembers(params: DeptUserTreeNode[], offspringNodes: DeptUserTreeNode[]) {
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
      await relateCustomFormMember({
        ...categorizedIds,
        customFormId: props.sourceId,
        customFormRole: activePermissionType.value,
      });
      Message.success(t('common.addSuccess'));
      memberDrawerVisible.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      addMemberLoading.value = false;
      searchData();
    }
  }

  watch(activePermissionType, () => {
    checkedRowKeys.value = [];
    crmTableRef.value?.clearCheckedRowKeys();
    searchData();
  });

  watch(tableRefreshId, () => {
    crmTableRef.value?.clearCheckedRowKeys();
    searchData();
  });

  watch(
    () => props.sourceId,
    () => {
      loadAdmins();
      searchData();
    }
  );

  onMounted(() => {
    loadAdmins();
    searchData();
  });
</script>

<style scoped lang="less"></style>
