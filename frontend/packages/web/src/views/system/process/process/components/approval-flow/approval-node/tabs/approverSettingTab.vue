<template>
  <n-form
    :rules="rules"
    class="process-setting-form"
    require-mark-placement="right"
    :model="nodeConfig"
    label-placement="top"
  >
    <n-form-item path="approvalType" :label="t('process.process.flow.approvalType')">
      <n-select
        v-model:value="nodeConfig.approvalType"
        :options="approvalTypeOptions"
        :placeholder="t('common.pleaseSelect')"
      />
    </n-form-item>

    <n-form-item path="name" :label="t('process.process.flow.nodeName')">
      <n-input v-model:value="nodeConfig.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
    </n-form-item>

    <template v-if="isManualApproval">
      <!-- 审批人 -->
      <n-form-item path="approverType" :label="t('process.process.flow.approver')">
        <n-select v-model:value="nodeConfig.approverType" :options="approverTypeOptions" />
      </n-form-item>

      <!-- 添加成员 -->
      <n-form-item
        v-if="nodeConfig.approverType === ApproverTypeEnum.SPECIFIED_MEMBER"
        path="approverList"
        :label="t('org.addMember')"
      >
        <!-- TODO lmy 添加成员样式 -->
        <CrmUserTagSelector
          v-model:value="nodeConfig.approverList"
          v-model:selected-list="nodeConfig.approverSelectedList"
        />
      </n-form-item>

      <!-- 指定层级 -->
      <n-form-item v-if="nodeConfig.approverType === ApproverTypeEnum.DIRECT_SUPERVISOR" path="approverList">
        <template #label>
          <!-- TODO lmy 示例 -->
          <span class="inline-flex items-center gap-[8px]">
            {{ t('process.process.flow.specifiedLevel') }}
            <n-tooltip trigger="hover" :delay="300">
              <template #trigger>
                <CrmIcon
                  :size="16"
                  type="iconicon_help_circle"
                  class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                />
              </template>
              {{ t('process.process.flow.directSupervisorTip') }}
            </n-tooltip>
          </span>
        </template>
        <n-select v-model:value="directSupervisorLevel" :options="approverLevelOptions" />
      </n-form-item>

      <n-form-item path="multiApproverMode" :label="t('process.process.flow.multiApprovalType')">
        <n-radio-group v-model:value="nodeConfig.multiApproverMode">
          <n-space vertical :size="8">
            <n-radio v-for="item in multiApproverModeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
              <span v-if="item.description" class="text-[var(--text-n4)]">{{ item.description }}</span>
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>

      <!-- TODO lmy 抄送人 异常处理 -->
      <n-form-item path="emptyApproverAction" :label="t('process.process.flow.exceptionHandling')">
        <n-radio-group v-model:value="nodeConfig.emptyApproverAction">
          <n-space vertical :size="8">
            <n-radio value="AUTO_PASS">{{ t('process.process.flow.exceptionHandling.autoPass') }}</n-radio>
            <n-radio value="AUTO_REJECT">{{ t('process.process.flow.autoReject') }}</n-radio>
            <n-radio value="TRANSFER_ADMIN">{{ t('process.process.flow.exceptionHandling.toAdmin') }}</n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>

      <n-form-item path="cc" :label="t('process.process.flow.ccUsers')"> </n-form-item>
    </template>
  </n-form>
</template>

<script setup lang="ts">
  import { computed, watch } from 'vue';
  import { FormRules, NForm, NFormItem, NInput, NRadio, NRadioGroup, NSelect, NSpace, NTooltip } from 'naive-ui';

  import { ApprovalTypeEnum, ApproverTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalActionNode, MultiApproverMode } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { approvalTypeOptions, approverTypeOptions } from '@/config/process';

  defineOptions({
    name: 'ApproverSettingTab',
  });

  const nodeConfig = defineModel<ApprovalActionNode>('nodeConfig', {
    required: true,
  });

  const { t } = useI18n();

  const approverLevelOptions = [
    {
      label: t('org.directSuperior'),
      value: '1',
    },
  ];

  const directSupervisorLevel = computed({
    get() {
      return nodeConfig.value.approverList[0] ?? '1';
    },
    set(value: string) {
      nodeConfig.value.approverList = [value];
    },
  });

  const multiApproverModeOptions: Array<{
    value: MultiApproverMode;
    label: string;
    description: string;
  }> = [
    {
      value: 'ALL',
      label: t('process.process.flow.multiApprovalType.all'),
      description: t('process.process.flow.multiApprovalType.all.description'),
    },
    {
      value: 'ANY',
      label: t('process.process.flow.multiApprovalType.majority'),
      description: t('process.process.flow.multiApprovalType.majority.description'),
    },
    {
      value: 'SEQUENTIAL',
      label: t('process.process.flow.multiApprovalType.sequential'),
      description: t('process.process.flow.multiApprovalType.sequential.description'),
    },
  ];

  const rules: FormRules = {
    name: [
      {
        required: true,
        message: t('common.notNull', { value: `${t('process.process.flow.nodeName')}` }),
        trigger: ['blur'],
      },
    ],
    approverList: [
      {
        trigger: ['change', 'blur'],
        validator: (_rule, value: unknown[]) => {
          if (nodeConfig.value.approverType !== ApproverTypeEnum.SPECIFIED_MEMBER) {
            return true;
          }

          if (Array.isArray(value) && value.length > 0) {
            return true;
          }

          return new Error(t('common.notNull', { value: t('org.addMember') }));
        },
      },
    ],
  };

  const isManualApproval = computed(() => nodeConfig.value.approvalType === ApprovalTypeEnum.MANUAL);

  watch(
    () => nodeConfig.value.approverType,
    (type, oldType) => {
      if (!oldType) {
        if (type === ApproverTypeEnum.DIRECT_SUPERVISOR && !nodeConfig.value.approverList.length) {
          nodeConfig.value.approverList = ['1'];
        }
        return;
      }

      if (type === oldType) {
        return;
      }

      nodeConfig.value.approverSelectedList = [];
      nodeConfig.value.approverList = type === ApproverTypeEnum.DIRECT_SUPERVISOR ? ['1'] : [];
    },
    {
      immediate: true,
    }
  );
</script>
