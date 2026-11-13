<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <CrmTable
      ref="crmTableRef"
      v-bind="propsRes"
      v-model:checked-row-keys="checkedRowKeys"
      class="crm-collaborator-table"
      :action-config="props.readonly ? undefined : actionConfig"
      noPagination
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @batch-action="handleBatchAction"
      @refresh="refreshCollaborators"
    >
      <template #actionLeft>
        <n-button
          v-if="!props.readonly"
          v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
          type="primary"
          @click="handleAddClick"
        >
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
      label-placement="top"
      require-mark-placement="right"
      class="collaborator-form"
      :rules="{
        member: [
          { trigger: ['input', 'blur'], required: true, message: t('common.notNull', { value: t('role.member') }) },
        ],
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
      <n-form-item path="permission" :label="t('role.permission')">
        <CrmRadioCardGroup v-model:value="form.permission" :options="collaborationOptions" />
      </n-form-item>
      <n-form-item path="expireTime">
        <template #label>
          <n-tooltip trigger="hover">
            <template #trigger>
              <div class="flex items-center gap-[4px]">
                {{ t('customer.validity') }}
                <CrmIcon
                  type="iconicon_help_circle"
                  class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                />
              </div>
            </template>
            {{ t('customer.validityTip') }}
          </n-tooltip>
        </template>
        <n-date-picker v-model:value="form.expireTime" type="datetime" clearable class="w-full" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { FormInst, NButton, NDatePicker, NForm, NFormItem, NTooltip, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CollaborationItem, CollaborationType } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmRadioCardGroup, { type CrmRadioCardOption } from '@/components/pure/crm-radio-card-group/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import {
    addCustomerCollaboration,
    batchDeleteCustomerCollaboration,
    deleteCustomerCollaboration,
    getUserOptions,
    updateCustomerCollaboration,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    sourceId: string; // 资源id
    collaborators: CollaborationItem[];
    readonly?: boolean; // 是否只读
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  function refreshCollaborators() {
    crmTableRef.value?.clearCheckedRowKeys();
    emit('refresh');
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  const removeLoading = ref(false);
  async function removeMember(row: any) {
    try {
      removeLoading.value = true;
      await deleteCustomerCollaboration(row.id);
      Message.success(t('common.removeSuccess'));
      refreshCollaborators();
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
    id: string;
    member: string | number | (string | number)[] | null;
    permission: CollaborationType;
    expireTime?: number | null;
  }>({
    id: '',
    member: null,
    permission: 'READ_ONLY',
  });
  const formRef = ref<FormInst>();
  const collaborationOptions = computed<CrmRadioCardOption[]>(() => [
    {
      value: 'COLLABORATION',
      label: t('customer.customerInfoOnly'),
      description: t('customer.collaborationDescription.infoOnly'),
    },
    {
      value: 'READ_ONLY',
      label: t('customer.readOnly'),
      description: t('customer.collaborationDescription.readOnly'),
    },
    {
      value: 'JOINT_FOLLOW',
      label: t('customer.jointFollow'),
      description: t('customer.collaborationDescription.jointFollow'),
    },
  ]);

  function handleAddMember() {
    formRef.value?.validate(async (errs) => {
      if (errs) return;
      try {
        addMemberLoading.value = true;
        if (isEdit.value) {
          await updateCustomerCollaboration({
            id: form.value.id,
            collaborationType: form.value.permission,
            expireTime: form.value.expireTime || null,
          });
          Message.success(t('common.updateSuccess'));
        } else {
          await addCustomerCollaboration({
            customerId: props.sourceId,
            userId: (form.value.member as string) || '',
            collaborationType: form.value.permission,
            expireTime: form.value.expireTime || null,
          });
          Message.success(t('common.addSuccess'));
        }
        addMemberModalVisible.value = false;
        refreshCollaborators();
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
      id: '',
      member: null,
      permission: 'READ_ONLY',
    };
    addMemberModalVisible.value = true;
  }

  function handleActionSelect(row: any, actionKey: string) {
    if (actionKey === 'edit') {
      isEdit.value = true;
      form.value = {
        id: row.id,
        member: row.userId,
        permission: row.collaborationType,
        expireTime: row.expireTime,
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
          Message.success(t('common.deleteSuccess'));
          refreshCollaborators();
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

  const collaborationTypeLabelMap: Record<CollaborationType, string> = {
    READ_ONLY: 'customer.readOnly',
    COLLABORATION: 'customer.customerInfoOnly',
    JOINT_FOLLOW: 'customer.jointFollow',
  };

  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.name'),
      key: 'userName',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: 'default',
      columnSelectorDisabled: true,
    },
    {
      title: t('role.department'),
      key: 'departmentName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: 'default',
    },
    {
      title: t('role.permission'),
      key: 'collaborationType',
      ellipsis: {
        tooltip: true,
      },
      width: 80,
      render: (row) => {
        return t(collaborationTypeLabelMap[row.collaborationType as CollaborationType]);
      },
    },
    {
      title: t('customer.validity'),
      key: 'expireTime',
      width: 160,
      render: (row) => (row.expireTime ? new Date(row.expireTime).toLocaleString() : t('customer.longTerm')),
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
      width: 80,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList:
            props.readonly || !hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE'])
              ? []
              : [
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
                      title: t('common.removeConfirmTitle', { name: characterLimit(row.userName) }),
                      content: t('customer.deleteMemberTip'),
                      positiveText: t('common.delete'),
                    },
                  },
                ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent } = useTable(undefined, {
    tableKey: TableKeyEnum.CUSTOMER_COLLABORATOR,
    showSetting: true,
    showPagination: false,
    columns,
    containerClass: '.crm-collaborator-table',
  });

  const disabledIds = computed(() => propsRes.value.data.map((e) => e.userId));

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
    () => props.collaborators,
    (list) => {
      propsRes.value.data = list.map((item) => ({
        ...item,
        createTime: item.createTime ? dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') : item.createTime,
        updateTime: item.updateTime ? dayjs(item.updateTime).format('YYYY-MM-DD HH:mm:ss') : item.updateTime,
      }));
      if (propsRes.value.crmPagination) propsRes.value.crmPagination.itemCount = list.length;
    },
    { immediate: true }
  );
</script>
