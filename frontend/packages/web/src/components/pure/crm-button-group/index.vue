<template>
  <div class="crm-button-group">
    <div v-for="(item, index) of props.list" :key="item.key" class="crm-button-item">
      <slot :name="item.slotName" :item="item" :index="index">
        <n-button v-bind="item" type="primary" text class="!p-0" @click="() => emit('select', item.key as string)">
          {{ item.label }}
        </n-button>
      </slot>
      <n-divider v-if="list[index + 1]" class="!mx-[8px]" vertical />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NDivider } from 'naive-ui';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';

  const props = defineProps<{
    list: ActionsItem[]; // 按钮组
  }>();

  const emit = defineEmits<{
    (e: 'select', key: string): void;
  }>();
</script>

<style scoped lang="less">
  .crm-button-group {
    @apply flex flex-nowrap items-center;
  }
</style>
