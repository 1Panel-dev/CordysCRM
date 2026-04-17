<template>
  <CrmProcessDrawer
    v-model:visible="visible"
    v-model:active-tab="activeTab"
    :tabList="tabList"
    @save="handleSave"
    @next-step="handleNextStep"
    @cancel="() => emit('cancel')"
  >
    <template>
      <!-- todo yuan   -->
      <div v-if="activeTab === 'process'"> process design </div>
      <moreSetting v-if="activeTab === 'moreSetting'" />
    </template>
  </CrmProcessDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmProcessDrawer from '@/components/business/crm-process-drawer/index.vue';
  import moreSetting from './moreSetting.vue';

  const { t } = useI18n();

  const props = defineProps<{
    sourceId?: string;
  }>();

  const emit = defineEmits<{
    (e: 'cancel'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const activeTab = ref('process');

  const tabList = [
    {
      name: 'process',
      tab: t('process.processDesign'),
    },
    {
      name: 'moreSetting',
      tab: t('process.processDesign.moreSetting'),
    },
  ];

  function handleNextStep() {
    const index = tabList.findIndex((item) => item.name === activeTab.value);
    if (index === tabList.length - 1) {
      return;
    }
    activeTab.value = tabList[index + 1].name;
  }

  function handleSave() {}
</script>

<style scoped></style>
