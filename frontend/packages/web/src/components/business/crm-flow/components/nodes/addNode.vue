<template>
  <div class="add-node">
    <CrmIcon type="iconicon_add_one" :size="16" class="add-node__icon" />
  </div>
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  defineOptions({
    name: 'AddNode',
  });
</script>

<style scoped lang="less">
  .add-node {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 24px;
    height: 24px;
    cursor: pointer;
  }
  .add-node__icon {
    color: var(--primary-4);
    &:hover {
      color: var(--primary-8);
    }
    &:active {
      color: var(--primary-0);
    }
  }
</style>
