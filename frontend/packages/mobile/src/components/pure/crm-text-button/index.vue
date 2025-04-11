<template>
  <div class="crm-text-button" :class="props.direction === 'column' ? 'flex-col' : ''" @click.stop="emit('click')">
    <CrmIcon
      v-if="props.icon"
      :name="props.icon"
      :width="props.iconSize"
      :height="props.iconSize"
      :color="props.color"
    />
    <div v-if="props.text" :style="{ color: props.color }">{{ props.text }}</div>
  </div>
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const props = withDefaults(
    defineProps<{
      text?: string;
      icon?: string;
      color?: string;
      direction?: 'row' | 'column';
      iconSize?: string;
    }>(),
    {
      text: '',
      icon: '',
      color: 'var(--primary-8)',
      direction: 'row',
      iconSize: '12px',
    }
  );
  const emit = defineEmits<{
    (e: 'click'): void;
  }>();
</script>

<style lang="less" scoped>
  .crm-text-button {
    @apply flex items-center;

    gap: 4px;
    font-size: 12px;
  }
</style>
