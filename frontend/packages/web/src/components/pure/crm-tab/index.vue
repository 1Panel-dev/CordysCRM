<template>
  <n-tabs
    v-model:value="activeTab"
    :type="props.type"
    :size="props.size"
    :class="`${props.noContent ? 'no-content' : ''}`"
    :bar-width="props.barWidth"
    @update:value="handleChange"
    @before-leave="handleBeforeLeave"
  >
    <n-tab-pane v-for="item of showTabs" :key="item.name" :name="item.name as string" :tab="item.tab">
      <slot :name="item.name" />
    </n-tab-pane>
  </n-tabs>
</template>

<script setup lang="ts">
  import { NTabPane, NTabs, TabPaneProps } from 'naive-ui';

  import { hasAnyPermission } from '@/utils/permission';

  export interface CrmTabListItem extends TabPaneProps {
    permission?: string[];
  }

  const props = withDefaults(
    defineProps<{
      tabList: CrmTabListItem[];
      type?: 'bar' | 'line' | 'card' | 'segment';
      size?: 'small' | 'medium' | 'large';
      noContent?: boolean;
      barWidth?: number;
      beforeLeave?: (newVal: string | number, oldVal: string | number | null) => boolean | Promise<boolean>;
    }>(),
    {
      size: 'medium',
    }
  );

  const activeTab = defineModel<string | number>('activeTab', {
    default: '',
  });

  const emit = defineEmits<{
    (e: 'change', value: string | number): void;
  }>();

  const showTabs = computed(() => {
    return props.tabList.filter((item) => {
      if (item.permission) {
        return hasAnyPermission(item.permission);
      }
      return true;
    });
  });

  function handleChange(value: string | number) {
    emit('change', value);
  }

  function handleBeforeLeave(name: string | number, oldName: string | number | null) {
    return props?.beforeLeave ? props?.beforeLeave(name, oldName) : true;
  }
</script>

<style lang="less" scoped>
  .no-content {
    :deep(.n-tab-pane) {
      padding: 0;
    }
  }
</style>
