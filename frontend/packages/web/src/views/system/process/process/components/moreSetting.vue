<template>
  <div class="process-more-setting flex w-full justify-center pt-[40px]">
    <n-form
      ref="formRef"
      class="process-more-setting-form"
      :model="form"
      label-placement="left"
      :label-width="100"
      require-mark-placement="left"
    >
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="submitterCanRevoke"
        :label="t('process.process.submitterAuthority')"
      >
        <n-checkbox v-model:checked="form.submitterCanRevoke" :disabled="readonly">
          <div class="flex items-center gap-[8px]">
            {{ t('process.process.allowSubmitterCancel') }}
            <n-tooltip trigger="hover">
              <template #trigger>
                <CrmIcon
                  type="iconicon_help_circle"
                  class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                />
              </template>
              {{ t('process.process.allowSubmitterCancelTip') }}
            </n-tooltip>
          </div>
        </n-checkbox>
      </n-form-item>
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="name"
        :label="t('process.process.approverAuthority')"
      >
        <div class="mt-[4px] flex flex-col gap-[4px]">
          <n-checkbox
            v-for="item of approverAuthorityList"
            :key="item.value"
            v-model:checked="form[item.value]"
            :disabled="readonly"
          >
            <div class="flex items-center gap-[8px]">
              {{ item.label }}
              <n-tooltip trigger="hover">
                <template #trigger>
                  <CrmIcon
                    type="iconicon_help_circle"
                    class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                  />
                </template>
                {{ t(item.tooltip) }}
              </n-tooltip>
            </div>
          </n-checkbox>
        </div>
      </n-form-item>
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="duplicateApproverRule"
        :label="t('process.process.autoApproval')"
      >
        <div class="mt-[4px] flex flex-col gap-[8px]">
          <div>{{ t('process.process.repeatApproval') }}</div>
          <n-radio-group v-model:value="form.duplicateApproverRule" :disabled="readonly">
            <div class="flex flex-col gap-[8px]">
              <n-radio v-for="item of autoApprovalList" :key="item.value" :value="item.value" :label="t(item.label)">
              </n-radio>
            </div>
          </n-radio-group>
        </div>
      </n-form-item>
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="requireComment"
        :label="t('process.process.approvalOpinion')"
      >
        <n-checkbox v-model:checked="form.requireComment" :disabled="readonly">
          {{ t('process.process.approvalRejectOpinion') }}
        </n-checkbox>
      </n-form-item>
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="approvalAuthority"
        :label="t('process.process.approvalAuthority')"
      >
        <n-data-table
          :columns="realColumns"
          :data="permissionData"
          :paging="false"
          class="approval-authority-table mt-[8px]"
          :pagination="false"
          :loading="loading"
        />
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup lang="ts">
  import { DataTableColumn, NCheckbox, NDataTable, NForm, NFormItem, NRadio, NRadioGroup, NTooltip } from 'naive-ui';

  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import {
    MoreSettingsParams,
    PermissionItem,
    ProcessStatusType,
    StatusPermissions,
  } from '@lib/shared/models/system/process';

  import { getApprovalPermissions } from '@/api/modules';
  import { defaultMoreConfig, processStatusOptions } from '@/config/process';
  import {
    matchPermissionBySuffix,
    processDefaultStatusPermissionMap,
  } from '@/config/process-default-status-permissions';

  const { t } = useI18n();

  const props = defineProps<{
    formType: string;
    needDetail?: boolean;
    readonly?: boolean;
  }>();

  const form = defineModel<MoreSettingsParams>('moreConfig', {
    default: () => ({
      ...defaultMoreConfig,
    }),
  });

  const approverAuthorityList: {
    value: 'allowBatchProcess' | 'allowWithdraw' | 'allowAddSign';
    label: string;
    tooltip: string;
  }[] = [
    {
      value: 'allowBatchProcess',
      label: t('process.process.approvalAuthority.batchAction'),
      tooltip: t('process.process.approvalAuthority.batchActionTip'),
    },
    {
      value: 'allowWithdraw',
      label: t('process.process.approvalAuthority.revokable'),
      tooltip: t('process.process.approvalAuthority.addTempApprover'),
    },
    {
      value: 'allowAddSign',
      label: t('process.process.approvalAuthority.allowAddSign'),
      tooltip: t('process.process.approvalAuthority.allowAddTempApprover'),
    },
  ];

  const autoApprovalList = [
    {
      value: 'FIRST_ONLY',
      label: 'process.process.autoApproval.firstNode',
    },
    {
      value: 'SEQUENTIAL_ALL',
      label: 'process.process.autoApproval.continuousNode',
    },
    {
      value: 'EACH',
      label: 'process.process.autoApproval.allNode',
    },
  ];

  const loading = ref(false);

  type ApprovalAuthorityRow = {
    status: ProcessStatusType;
    statusLabel: string;
    permissions: Record<string, boolean>;
  };

  function isReadPermission(permissionId: string) {
    return permissionId === 'READ' || permissionId.endsWith(':READ');
  }

  function getReadPermissionId(row: ApprovalAuthorityRow) {
    return Object.keys(row.permissions).find((key) => isReadPermission(key));
  }

  function flattenStatusPermissions(data: ApprovalAuthorityRow[]): StatusPermissions[] {
    const result: StatusPermissions[] = [];

    data.forEach((item) => {
      const { status, permissions } = item;
      Object.entries(permissions).forEach(([permissionKey, enabled]) => {
        result.push({
          approvalStatus: status,
          permission: permissionKey,
          enabled: Boolean(enabled),
        });
      });
    });

    return result;
  }

  const permissionData = ref<ApprovalAuthorityRow[]>([]);
  function updateRowPermission(row: ApprovalAuthorityRow, permissionId: string, checked: boolean) {
    const readPermissionId = getReadPermissionId(row);

    if (isReadPermission(permissionId)) {
      if (!checked) {
        Object.keys(row.permissions).forEach((key) => {
          row.permissions[key] = false;
        });
        return;
      }

      row.permissions[permissionId] = true;
      return;
    }

    row.permissions[permissionId] = checked;

    if (!readPermissionId) {
      return;
    }

    if (checked) {
      row.permissions[readPermissionId] = true;
    }
  }

  const permission = ref<PermissionItem[]>([]);

  const realColumns = computed(() => {
    return [
      {
        title: t('common.status'),
        key: 'status',
        minWidth: 70,
        render: (row: ApprovalAuthorityRow) => row.statusLabel,
      },
      ...permission.value.map(
        (item) =>
          ({
            key: item.id,
            title: item.name,
            minWidth: 70,
            render: (row: ApprovalAuthorityRow) =>
              h(NCheckbox, {
                checked: Boolean(row.permissions[item.id]),
                disabled: props.readonly,
                onUpdateChecked: (checked: boolean) => {
                  updateRowPermission(row, item.id, checked);
                  form.value.statusPermissions = flattenStatusPermissions(permissionData.value);
                },
              }),
          } as DataTableColumn<ApprovalAuthorityRow>)
      ),
    ];
  });

  function createApprovalAuthorityRows(formType: string, data: PermissionItem[]): ApprovalAuthorityRow[] {
    const moduleDefaultConfig = processDefaultStatusPermissionMap[formType] || {};

    return processStatusOptions
      .filter((item) => item.value !== ProcessStatusEnum.NONE)
      .map((item) => {
        const enabledSuffixList = moduleDefaultConfig[item.value as ProcessStatusType] || [];
        return {
          status: item.value as ProcessStatusType,
          statusLabel: item.label,
          permissions: data.reduce((acc, permissionItem: PermissionItem) => {
            acc[permissionItem.id] = enabledSuffixList.some((suffix) =>
              matchPermissionBySuffix(permissionItem.id, suffix)
            );
            return acc;
          }, {} as Record<string, boolean>),
        };
      });
  }

  function aggregateStatusPermissions(flatData: StatusPermissions[]): ApprovalAuthorityRow[] {
    const statusMap = new Map<ProcessStatusType, Record<string, boolean>>();
    const processStatusOptionsMap = new Map(
      processStatusOptions.filter((e) => e.value !== ProcessStatusEnum.NONE).map((item) => [item.value, item])
    );

    flatData.forEach((item) => {
      if (!statusMap.has(item.approvalStatus)) {
        statusMap.set(item.approvalStatus, {});
      }
      statusMap.get(item.approvalStatus)![item.permission] = item.enabled;
    });

    return Array.from(statusMap.entries()).map(([status, permissions]) => ({
      status,
      statusLabel: processStatusOptionsMap.get(status)?.label ?? '-',
      permissions,
    }));
  }

  async function initPermissionData() {
    try {
      const result = await getApprovalPermissions(props.formType);
      permission.value = result.permissions.filter((e) => !e.id.includes(':ADD') && !e.id.includes(':APPROVAL'));
      if (props.needDetail) {
        permissionData.value = aggregateStatusPermissions(form.value.statusPermissions);
      } else {
        permissionData.value = createApprovalAuthorityRows(props.formType, permission.value);
      }
      form.value.statusPermissions = flattenStatusPermissions(permissionData.value);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    [() => props.formType, () => form.value.permissions],
    () => {
      initPermissionData();
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less">
  .process-more-setting-form {
    .n-form-item-label {
      margin-right: 32px;
      font-weight: 600;
      color: var(--text-n1);
    }
    .process-more-setting {
      @apply h-full;

      background: var(--text-n10);
    }
    .approval-authority-table {
      .n-data-table-th {
        background: var(--text-n9);
      }
    }
  }
</style>
