<template>
  <div class="base-flow-node large-box-shadow" :class="[`base-flow-node--${nodeType}`, { 'is-selected': selected }]">
    <div class="base-flow-node__header">
      <div class="base-flow-node__title-wrap">
        <div
          class="flex h-[24px] w-[24px] items-center justify-center rounded-[var(--border-radius-small)]"
          :style="`background-color: ${props.icon.backgroundColor}`"
        >
          <CrmIcon :type="props.icon.type" :size="16" class="text-[var(--text-n10)]" />
        </div>
        <span class="base-flow-node__title">{{ name }}</span>
      </div>
      <div class="base-flow-node__header-extra">
        <n-tooltip v-if="deletable" :delay="300" trigger="hover">
          <template #trigger>
            <CrmIcon
              type="iconicon_close"
              :size="16"
              class="base-flow-node__delete-icon cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-8)]"
              @click="handleDelete"
            />
          </template>
          <span> {{ t('crmFlow.deleteNode') }} </span>
        </n-tooltip>
      </div>
    </div>
    <div v-if="props.nodeType !== 'end' && props.showContent" class="base-flow-node__content">
      <span class="base-flow-node__text">{{ description }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  defineOptions({
    name: 'BaseFlowNode',
  });

  const props = withDefaults(
    defineProps<{
      name: string;
      description?: string;
      nodeType?: string;
      selected?: boolean;
      deletable?: boolean;
      showContent?: boolean;
      icon: {
        type: string;
        backgroundColor: string;
      };
    }>(),
    {
      description: '',
      nodeType: 'action',
      selected: false,
      deletable: false,
      showContent: true,
    }
  );

  const emit = defineEmits<{
    (event: 'delete'): void;
  }>();

  const { t } = useI18n();

  function handleDelete() {
    emit('delete');
  }
</script>

<style scoped lang="less">
  .base-flow-node {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 16px;
    border: 1px solid transparent;
    border-radius: 8px;
    background: var(--text-n10);
    transition: border-color 0.2s ease;
    &:hover {
      border: 1px solid var(--primary-1);
    }
    &.is-selected {
      border-color: var(--primary-0);
    }
  }
  .base-flow-node__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
  }
  .base-flow-node__header-extra,
  .base-flow-node__title-wrap {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .base-flow-node__title {
    font-size: 16px;
    font-weight: 500;
    color: var(--text-n1);
  }
  .base-flow-node__content {
    padding: 5px 12px;
    border-radius: 4px;
    background: var(--text-n9);
  }
  .base-flow-node__text {
    color: var(--text-n2);
  }
</style>
