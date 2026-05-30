<template>
  <n-collapse v-model:expanded-names="expandedNames" class="crm-collapse" @update:expanded-names="updateExpanded">
    <n-collapse-item :title="props.title" :name="props.nameKey">
      <template #header="{ collapsed }">
        <slot name="header" :collapsed="collapsed">
          <div class="w-full">
            <div class="crm-collapse-item-header-title">
              <n-tooltip trigger="hover">
                <template #trigger>
                  {{ props.title }}
                </template>
                {{ props.title }}
              </n-tooltip>
            </div>
          </div>
        </slot>
      </template>
      <slot></slot>
      <template #header-extra="{ collapsed }">
        <div class="flex items-center justify-center gap-[8px]">
          <slot name="headerExtraLeft" :collapsed="collapsed"></slot>
          <n-button v-if="props.showExpandBtn" quaternary class="!p-0 !text-[var(--text-n4)]">
            <CrmIcon
              class="text-[var(--text-n4)]"
              :type="`${collapsed ? 'iconicon_chevron_down' : 'iconicon_chevron_right'}`"
              :size="16"
            />
          </n-button>
        </div>
      </template>
    </n-collapse-item>
  </n-collapse>
</template>

<script setup lang="ts">
  import { NButton, NCollapse, NCollapseItem, NTooltip } from 'naive-ui';

  interface CollapseProps {
    nameKey: string;
    title?: string;
    defaultExpand?: boolean;
    showExpandBtn?: boolean;
  }

  const props = withDefaults(defineProps<CollapseProps>(), {
    defaultExpand: false, // 默认展开
    showExpandBtn: true, // 显示折叠按钮
  });

  const emit = defineEmits<{
    (e: 'expand', expandedNames: Array<string | number> | string | number | null): void;
  }>();

  function updateExpanded(expandedNames: Array<string | number> | string | number | null) {
    emit('expand', expandedNames);
  }

  const expandedNames = ref<string[]>([]);

  watch(
    () => props.defaultExpand,
    (val) => {
      if (val && !expandedNames.value.includes(props.nameKey)) {
        expandedNames.value.push(props.nameKey);
      } else {
        expandedNames.value = [];
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less">
  .crm-collapse {
    &.n-collapse {
      padding: 24px;
      background: var(--text-n10);
      .n-collapse-item-arrow {
        display: none !important;
      }
      .crm-collapse-item-header-title {
        color: var(--text-n1);
        @apply font-medium;
      }
    }
  }
</style>
