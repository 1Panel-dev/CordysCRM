<template>
  <CrmCard hide-footer>
    <CrmTable
      v-bind="propsRes"
      v-model:checked-row-keys="checkedRowKeys"
      :action-config="actionConfig"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @batch-action="handleBatchAction"
    >
      <template #actionLeft>
        <n-button v-permission="['CUSTOMER_MANAGEMENT:UPDATE']" type="primary" @click="handleAddClick">
          {{ t('role.addMember') }}
        </n-button>
      </template>
    </CrmTable>
  </CrmCard>
  <CrmModal
    v-model:show="addMemberModalVisible"
    :title="isEdit ? t('org.updateMember') : t('role.addMember')"
    :ok-loading="addMemberLoading"
    :positive-text="isEdit ? t('common.update') : t('common.add')"
    @confirm="handleAddMember"
  >
    <n-form
      ref="formRef"
      :model="form"
      label-placement="left"
      label-width="auto"
      require-mark-placement="left"
      label-align="right"
      :rules="{
        member: [{ required: true, message: t('common.notNull', { value: t('role.member') }) }],
      }"
    >
      <n-form-item path="member" :label="t('role.member')">
        <CrmUserSelect
          v-model:value="form.member"
          value-field="id"
          label-field="name"
          mode="remote"
          :fetch-api="getUserOptions"
          max-tag-count="responsive"
          :disabled="isEdit"
          :disabled-ids="disabledIds"
        />
      </n-form-item>
      <n-form-item path="permission" feedback-class="hidden">
        <template #label>
          <n-tooltip trigger="hover">
            <template #trigger>
              <div class="flex items-center gap-[4px]">
                {{ t('role.permission') }}
                <CrmIcon type="iconicon_help_circle" class="cursor-pointer hover:text-[var(--primary-1)]" />
              </div>
            </template>
            {{ t('customer.cooperationTip') }}
          </n-tooltip>
        </template>
        <n-tabs v-model:value="form.permission" type="segment" class="collaborator-tabs">
          <n-tab-pane name="READ_ONLY" :tab="t('customer.readOnly')"></n-tab-pane>
          <n-tab-pane name="COLLABORATION" :tab="t('customer.cooperation')"></n-tab-pane>
        </n-tabs>
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { FormInst, NButton, NForm, NFormItem, NTabPane, NTabs, NTooltip, useMessage } from 'naive-ui';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import {
    addCustomerCollaboration,
    batchDeleteCustomerCollaboration,
    deleteCustomerCollaboration,
    getCustomerCollaborationList,
    updateCustomerCollaboration,
  } from '@/api/modules/customer';
  import { getUserOptions } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string; // 资源id
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const tableRefreshId = ref(0);
  const removeLoading = ref(false);
  async function removeMember(row: any) {
    try {
      removeLoading.value = true;
      await deleteCustomerCollaboration(row.id);
      Message.success(t('common.removeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      removeLoading.value = false;
    }
  }

  const isEdit = ref(false);
  const addMemberModalVisible = ref(false);
  const addMemberLoading = ref(false);
  const form = ref<{
    member: string | number | (string | number)[];
    permission: CollaborationType;
  }>({
    member: '',
    permission: 'READ_ONLY',
  });
  const formRef = ref<FormInst>();

  function handleAddMember() {
    formRef.value?.validate(async (errs) => {
      if (errs) return;
      try {
        addMemberLoading.value = true;
        if (isEdit.value) {
          await updateCustomerCollaboration({
            id: (form.value.member as string) || '',
            collaborationType: form.value.permission,
          });
          Message.success(t('common.updateSuccess'));
        } else {
          await addCustomerCollaboration({
            customerId: props.sourceId,
            userId: (form.value.member as string) || '',
            collaborationType: form.value.permission,
          });
          Message.success(t('common.addSuccess'));
        }
        addMemberModalVisible.value = false;
        tableRefreshId.value += 1;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      } finally {
        addMemberLoading.value = false;
      }
    });
  }

  function handleAddClick() {
    isEdit.value = false;
    form.value = {
      member: '',
      permission: 'READ_ONLY',
    };
    addMemberModalVisible.value = true;
  }

  function handleActionSelect(row: any, actionKey: string) {
    if (actionKey === 'edit') {
      isEdit.value = true;
      form.value = {
        member: row.userId,
        permission: row.collaborationType,
      };
      addMemberModalVisible.value = true;
    } else if (actionKey === 'pop-delete') {
      removeMember(row);
    }
  }

  const checkedRowKeys = ref<string[]>([]);
  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('customer.batchDeleteMemberTip', { number: checkedRowKeys.value.length }),
      content: t('customer.deleteMemberTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchDeleteCustomerCollaboration(checkedRowKeys.value);
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'delete':
        handleBatchDelete();
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.name'),
      key: 'userName',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('role.department'),
      key: 'departmentName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('role.permission'),
      key: 'collaborationType',
      ellipsis: {
        tooltip: true,
      },
      width: 80,
      render: (row) => {
        return t(row.collaborationType === 'READ_ONLY' ? 'customer.readOnly' : 'customer.cooperation');
      },
    },
    {
      title: t('common.creator'),
      key: 'createUserName',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 150,
    },
    {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: [
            {
              key: 'edit',
              label: t('common.edit'),
            },
            {
              label: t('common.delete'),
              key: 'delete',
              danger: true,
              popConfirmProps: {
                loading: removeLoading.value,
                title: t('common.removeConfirmTitle', { name: row.userName }),
                content: t('customer.deleteMemberTip'),
                positiveText: t('common.delete'),
              },
            },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getCustomerCollaborationList, {
    tableKey: TableKeyEnum.CUSTOMER_COLLABORATOR,
    showSetting: true,
    showPagination: false,
    columns,
  });

  const disabledIds = computed(() => propsRes.value.data.map((e) => e.userId));

  function initData() {
    setLoadListParams({
      customerId: props.sourceId,
    });
    loadList();
  }

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        key: 'delete',
        label: t('common.batchDelete'),
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
    ],
  };

  watch(
    () => tableRefreshId.value,
    () => {
      initData();
    }
  );

  onBeforeMount(() => {
    initData();
  });
</script>

<style lang="less">
  .collaborator-tabs {
    .n-tab-pane {
      @apply hidden;
    }
  }
</style>
