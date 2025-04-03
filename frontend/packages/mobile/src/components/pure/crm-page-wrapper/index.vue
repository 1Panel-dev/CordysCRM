<template>
  <div class="crm-page-wrapper">
    <CrmPageHeader :title="props.title" :back-route-name="props.backRouteName" />
    <div class="crm-page-content">
      <slot />
    </div>
    <div v-if="$slots.footer" class="crm-page-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
  import CrmPageHeader from '@/components/pure/crm-page-header/index.vue';

  const props = defineProps<{
    title: string;
    backRouteName?: string;
  }>();
</script>

<style lang="less" scoped>
  .crm-page-wrapper {
    @apply flex flex-col;

    height: 100vh;
    background-color: var(--text-n10);
    .crm-page-content {
      @apply flex flex-1 flex-col;

      overflow: auto;
      margin-top: 48px;
      background-color: transparent;
    }
    .crm-page-footer {
      padding: 16px;
      background-color: var(--text-n10);
    }
  }
</style>
