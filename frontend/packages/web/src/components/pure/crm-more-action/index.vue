<template>
  <n-dropdown
    v-if="props.options.length"
    trigger="click"
    :options="props.options"
    :render-option="renderOptions"
    class="crm-dropdown"
    :placement="props.placement"
    @select="handleSelect"
  >
    <n-button type="primary" size="small" ghost class="h-[24px] w-[24px] !p-0">
      <template #icon>
        <n-icon>
          <CrmIcon type="icon-icon_more_outlined" :size="16" class="mt-[1px] text-[var(--primary-8)]" />
        </n-icon>
      </template>
    </n-button>
  </n-dropdown>
</template>

<script setup lang="ts">
  import { VNodeChild } from 'vue';
  import { DropdownGroupOption, NButton, NDropdown, NIcon, PopoverTrigger } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import type { ActionsItem } from './type';

  const props = withDefaults(
    defineProps<{
      options: ActionsItem[];
      trigger?: PopoverTrigger;
      placement?:
        | 'top-start'
        | 'top'
        | 'top-end'
        | 'right-start'
        | 'right'
        | 'right-end'
        | 'bottom-start'
        | 'bottom'
        | 'bottom-end'
        | 'left-start'
        | 'left'
        | 'left-end';
    }>(),
    {
      placement: 'bottom-start',
    }
  );

  const emit = defineEmits<{
    (e: 'select', currentAction: ActionsItem): void;
  }>();

  function handleSelect(key: string | number) {
    const item = props.options.find((e: ActionsItem) => e.key === key);
    if (item) {
      emit('select', item);
    }
  }

  function renderOptions(dprops: { node: VNode; option: ActionsItem | DropdownGroupOption }) {
    const { option } = dprops;
    const icon = option.icon as unknown as string;
    return h(
      'div',
      {
        class: `crm-dropdown-btn ${
          option.danger ? 'hover:bg-[var(--error-5)] text-[var(--error-red)]' : 'hover:bg-[var(--primary-7)]'
        }`,
        onClick: () => handleSelect(option.key as string),
      },
      {
        default: () => {
          const content = [];
          if (icon) {
            content.push(
              h(CrmIcon, {
                size: 16,
                type: icon,
                class: `mr-[4px] ${option.danger ? 'text-[var(--error-red)] ' : undefined}`,
              })
            );
          }
          content.push(option.label as VNodeChild);
          return content;
        },
      }
    );
  }
</script>

<style lang="less">
  .crm-dropdown-btn {
    padding: 0 14px;
    min-width: 80px;
    height: 28px;
    @apply flex cursor-pointer items-center rounded;
  }
</style>
