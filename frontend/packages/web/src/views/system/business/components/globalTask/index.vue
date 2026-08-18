<template>
  <CrmCard hide-footer no-content-padding class="flex-1" :special-height="licenseStore.expiredDuring ? 128 : 64">
    <n-tabs v-model:value="activeMenu" placement="left" type="line" class="global-task-tabs h-full">
      <n-tab-pane name="taskList" :tab="t('system.business.globalTask.taskList')">
        <TaskList />
      </n-tab-pane>
      <n-tab-pane name="executionRecord" :tab="t('system.business.globalTask.executionRecord')">
        <ExecutionRecord />
      </n-tab-pane>
    </n-tabs>
  </CrmCard>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NTabPane, NTabs } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import ExecutionRecord from './executionRecord.vue';
  import TaskList from './taskList.vue';

  import useLicenseStore from '@/store/modules/setting/license';

  const { t } = useI18n();
  const activeMenu = ref('taskList');
  const licenseStore = useLicenseStore();
</script>

<style scoped lang="scss">
  .global-task-tabs {
    :deep(.n-tabs-tab__label) {
      width: 130px;
    }
    :deep(.n-tab-pane) {
      padding: 24px 24px 0;
      height: 100%;
    }
  }
</style>
