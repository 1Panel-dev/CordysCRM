<template>
  <n-dropdown
    trigger="click"
    :options="props.options"
    :render-label="renderLabel"
    class="crm-dropdown"
    :node-props="getNodeProps"
    :placement="props.placement"
    @select="handleSelect"
  >
    <n-button type="primary" size="small" ghost class="h-[24px] w-[24px] !p-0" @click.stop="(e) => emit('click', e)">
      <template #icon>
        <n-icon>
          <CrmIcon type="iconicon_ellipsis" :size="16" class="mt-[1px] text-[var(--primary-8)]" />
        </n-icon>
      </template>
    </n-button>
  </n-dropdown>
</template>

<script setup lang="ts">
  import { HTMLAttributes, VNodeChild } from 'vue';
  import {
    DropdownGroupOption,
    DropdownOption,
    MenuNodeProps,
    NButton,
    NDropdown,
    NIcon,
    PopoverTrigger,
  } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import type { ActionsItem } from './type';

  const props = withDefaults(
    defineProps<{
      options: ActionsItem[];
      trigger?: PopoverTrigger;
      nodeProps?: (option: DropdownOption | DropdownGroupOption) => HTMLAttributes;
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
    (e: 'click', event: MouseEvent): void;
  }>();

  function handleSelect(key: string | number) {
    const item = props.options.find((e: ActionsItem) => e.key === key);
    if (item) {
      emit('select', item);
    }
  }

  function renderLabel(option: DropdownOption) {
    const icon = option.iconType as string;

    return h(
      'div',
      {
        class: `crm-dropdown-btn w-full ${option.danger ? ' text-[var(--error-red)]' : ''}`,
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

  function getNodeProps(option: DropdownOption | DropdownGroupOption) {
    const baseClass = option?.danger
      ? 'crm-dropdown-btn hover:bg-[var(--error-5)]'
      : 'crm-dropdown-btn hover:bg-[var(--primary-7)]';

    if (props.nodeProps) {
      const nodeHtmlProps: HTMLAttributes =
        typeof props.nodeProps === 'function' ? props.nodeProps(option) : props.nodeProps;

      return {
        ...(nodeHtmlProps as MenuNodeProps),
        class: `${baseClass} ${nodeHtmlProps?.class || ''}`, // 合并 class 并避免空值
      };
    }

    return { class: baseClass };
  }
</script>

<style lang="less">
  .crm-dropdown-btn {
    padding: 0 8px;
    min-width: 60px;
    height: 30px;
    @apply flex cursor-pointer items-center rounded;
  }
</style>
