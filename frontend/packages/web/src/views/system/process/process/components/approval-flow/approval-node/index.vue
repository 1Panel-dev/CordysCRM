<template>
  <div>
    <CrmTab v-if="isManualApproval" v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />

    <div class="p-[16px]">
      <ApproverSettingTab v-if="!isManualApproval || activeTab === 'approver'" v-model:node-config="nodeConfig" />
      <FormPermissionTab v-else-if="activeTab === 'formPermission'" />
      <AfterApprovalTab v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, watch } from 'vue';

  import { ApprovalTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalActionNode } from '@lib/shared/models/system/process';

  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import AfterApprovalTab from './tabs/afterApprovalTab.vue';
  import ApproverSettingTab from './tabs/approverSettingTab.vue';
  import FormPermissionTab from './tabs/formPermissionTab.vue';

  defineOptions({
    name: 'ApprovalActionNodeForm',
  });

  const nodeConfig = defineModel<ApprovalActionNode>('node', {
    required: true,
  });

  const { t } = useI18n();
  const activeTab = ref('approver');
  const isManualApproval = computed(() => nodeConfig.value.approvalType === ApprovalTypeEnum.MANUAL);

  const tabList = [
    {
      name: 'approver',
      tab: t('process.process.flow.approverSetting'),
    },
    {
      name: 'formPermission',
      tab: t('process.process.flow.formPermission'),
    },
    {
      name: 'afterApproval',
      tab: t('process.process.flow.afterApproval'),
    },
  ];

  watch(isManualApproval, (isManual) => {
    if (!isManual) {
      activeTab.value = 'approver';
    }
  });
</script>

<style lang="less" scoped>
  :deep(.n-tabs-wrapper) {
    width: 100%;
    .n-tabs-tab-wrapper {
      flex: 1;
      justify-content: center;
    }
  }
</style>
