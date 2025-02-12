<template>
  <n-scrollbar x-scrollable content-class="flex h-full !w-full bg-[var(--text-n9)]" content-style="min-width: 800px">
    <div class="crm-form-design--left"><fieldComponents @select="handleFieldSelect" /></div>
    <div class="crm-form-design--center">
      <div class="crm-form-design--center-content"><formComposition ref="formCompositionRef" /></div>
    </div>
    <div class="crm-form-design--right">i am right</div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';

  import fieldComponents from './components/fieldComponents.vue';
  import formComposition from './components/formComposition/index.vue';

  import { FieldTypeEnum } from './enum';

  const formCompositionRef = ref<InstanceType<typeof formComposition>>();

  function handleFieldSelect(type: FieldTypeEnum, name: string) {
    formCompositionRef.value?.addItem(type, name);
  }
</script>

<style lang="less" scoped>
  .crm-form-design--left,
  .crm-form-design--right {
    @apply h-full;

    width: 280px;
    background-color: var(--text-n10);
  }
  .crm-form-design--left {
    padding: 16px;
  }
  .crm-form-design--center {
    @apply h-full flex-1 bg-transparent;

    padding: 16px;
    .crm-form-design--center-content {
      @apply h-full;

      border-radius: var(--border-radius-small);
      background-color: var(--text-n10);
    }
  }
</style>
