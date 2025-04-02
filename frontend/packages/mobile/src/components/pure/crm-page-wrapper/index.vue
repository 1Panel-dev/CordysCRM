<template>
  <div class="crm-page-wrapper">
    <CrmPageHeader :title="props.title" :back-route-name="props.backRouteName" />
    <div class="crm-page-content">
      <slot />
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
      @apply flex-1;

      overflow: auto;
      margin-top: 48px;
    }
  }
</style>
