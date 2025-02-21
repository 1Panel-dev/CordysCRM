<template>
  <n-collapse
    class="crm-collapse"
    :default-expanded-names="props.defaultExpand ? props.nameKey : ''"
    @update:expanded-names="updateExpanded"
  >
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
        <div class="flex items-center justify-center">
          <n-button quaternary class="text-btn-secondary !p-0 !text-[var(--text-n4)]">
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

  const props = withDefaults(
    defineProps<{
      nameKey: string;
      title?: string;
      defaultExpand?: boolean;
    }>(),
    {
      defaultExpand: false,
    }
  );

  const emit = defineEmits<{
    (e: 'expand', expandedNames: Array<string | number> | string | number | null): void;
  }>();

  function updateExpanded(expandedNames: Array<string | number> | string | number | null) {
    emit('expand', expandedNames);
  }
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
