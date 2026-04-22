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
        path="name"
        :label="t('process.process.submitterAuthority')"
      >
        <n-checkbox v-model:checked="form.submitterAuthority">
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
        <n-checkbox-group v-model:value="form.approverAuthority" class="mt-[4px]">
          <div class="flex flex-col gap-[8px]">
            <n-checkbox v-for="item of approverAuthorityList" :key="item.value" :value="item.value">
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
        </n-checkbox-group>
      </n-form-item>
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="autoApproval"
        :label="t('process.process.autoApproval')"
      >
        <div class="mt-[4px] flex flex-col gap-[8px]">
          <div>{{ t('process.process.repeatApproval') }}</div>
          <n-radio-group v-model:value="form.autoApproval">
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
        path="approvalOpinion"
        :label="t('process.process.approvalOpinion')"
      >
        <n-checkbox v-model:checked="form.approvalOpinion">
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
          :single-line="false"
          :columns="columns"
          :data="data"
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
  // todo xinxinwu
  import { ref } from 'vue';
  import {
    DataTableColumn,
    NCheckbox,
    NCheckboxGroup,
    NDataTable,
    NForm,
    NFormItem,
    NRadio,
    NRadioGroup,
    NTooltip,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { defaultMoreConfig } from '@/config/process';

  const { t } = useI18n();

  const form = defineModel<Record<string, any>>('moreConfig', {
    default: () => ({
      ...defaultMoreConfig,
    }),
  });

  const approverAuthorityList = [
    {
      value: 'allowBatchProcess',
      label: t('process.process.approvalAuthority.batchAction'),
      tooltip: t('process.process.approvalAuthority.batchActionTip'),
    },
    {
      value: 'allowRevoke',
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
      value: 'firstNodeApproval',
      label: 'process.process.autoApproval.firstNode',
    },
    {
      value: 'continuousNodeApproval',
      label: 'process.process.autoApproval.continuousNode',
    },
    {
      value: 'allNodeApproval',
      label: 'process.process.autoApproval.allNode',
    },
  ];

  const loading = ref(false);
  const columns: DataTableColumn[] = [];
  const data: any[] = [];
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
  }
</style>
