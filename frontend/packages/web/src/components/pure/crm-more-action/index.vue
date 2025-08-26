<template>
  <n-dropdown
    v-if="moreOptions.length"
    :trigger="props.trigger"
    :options="moreOptions"
    :render-label="renderLabel"
    class="crm-dropdown"
    :node-props="getNodeProps"
    :placement="props.placement"
    size="small"
    @select="handleSelect"
    @update-show="handleUpdateShow"
    @clickoutside="clickOutSide"
  >
    <slot>
      <n-button
        type="primary"
        :size="props.size"
        ghost
        :class="`crm-more-action--size-${props.size} h-[20px] px-[3px]`"
        @click.stop="(e) => emit('click', e)"
      >
        <template #icon>
          <CrmIcon type="iconicon_ellipsis" :size="16" class="mt-[1px] text-[var(--primary-8)]" />
        </template>
      </n-button>
    </slot>
  </n-dropdown>
</template>

<script setup lang="ts">
  import { HTMLAttributes } from 'vue';
  import {
    DropdownGroupOption,
    DropdownOption,
    MenuNodeProps,
    NButton,
    NDropdown,
    NTooltip,
    PopoverTrigger,
  } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmPopConfirm, { CrmPopConfirmProps } from '@/components/pure/crm-pop-confirm/index.vue';

  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import type { ActionsItem } from './type';
  import { Size } from 'naive-ui/es/button/src/interface';

  const slots = useSlots();
  const props = withDefaults(
    defineProps<{
      options: ActionsItem[];
      trigger?: PopoverTrigger;
      nodeProps?: (option: DropdownOption | DropdownGroupOption) => HTMLAttributes;
      size?: Size;
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
      trigger: 'hover',
      size: 'small',
    }
  );

  const emit = defineEmits<{
    (e: 'select', currentAction: ActionsItem): void;
    (e: 'click', event: MouseEvent): void;
    (e: 'updateShow', show: boolean): void;
    (e: 'close'): void;
    (e: 'popSelect', key: string, done?: () => void): void;
    (e: 'popCancel'): void;
  }>();

  function handleSelect(key: string | number) {
    const item = props.options.find((e: ActionsItem) => e.key === key);
    if (item) {
      emit('select', item);
    }
  }

  function handleUpdateShow(show: boolean) {
    emit('updateShow', show);
  }
  const popShow = ref<Record<string, boolean>>({});

  function cancel() {
    // 关闭所有弹出框
    Object.keys(popShow.value).forEach((key) => {
      popShow.value[key] = false;
    });
  }

  function renderLabel(option: DropdownOption) {
    const icon = option.iconType as string;
    return h(
      'div',
      {
        class: `crm-dropdown-btn w-full ${option.danger ? ' text-[var(--error-red)]' : ''} ${
          option.disabled ? 'cursor-not-allowed text-[var(--text-n6)]' : 'cursor-pointer'
        }`,
      },
      {
        default: () => {
          const content = [];
          if (option.render) {
            content.push(option.render);
          } else {
            if (icon) {
              content.push(
                h(CrmIcon, {
                  size: 16,
                  type: icon,
                  class: `mr-[4px] ${option.danger ? 'text-[var(--error-red)]' : ''}`,
                })
              );
            }

            // 处理带有popConfirm的情况
            let labelContent;
            if (option?.popConfirmProps) {
              labelContent = h(
                CrmPopConfirm,
                {
                  'show': popShow.value[option.key as string],
                  ...((option?.popConfirmProps as CrmPopConfirmProps) || {}),
                  'placement': 'bottom-end',
                  'onConfirm': () => emit('popSelect', `pop-${option.key}`, cancel),
                  'onCancel': () => emit('popCancel'),
                  'onUpdate:show': (val: boolean) => {
                    popShow.value[option.key as string] = val;
                  },
                },
                {
                  default: () =>
                    h(
                      'div',
                      {
                        class: 'flex-1',
                        onClick: (e: Event) => e.stopPropagation(),
                      },
                      option.label as string
                    ),
                  content: () => {
                    const slotName = option.popSlotContent as string;
                    const slotFn = (slots as any)[slotName];
                    return slotName && slotFn ? slotFn({ key: option.key }) : null;
                  },
                }
              );
            } else {
              labelContent = h('div', { class: 'flex-1' }, option.label as string);
            }
            content.push(
              h(
                NTooltip,
                {
                  delay: 300,
                  flip: true,
                  disabled: !option.tooltipContent,
                  to: 'body',
                },
                {
                  trigger: () => h('div', { class: 'flex-1' }, labelContent),
                  default: () => option.tooltipContent,
                }
              )
            );
          }
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

  function cleanDividers(actions: ActionsItem[]) {
    const cleaned: ActionsItem[] = [];
    let prevIsDivider = false;

    actions.forEach((action) => {
      if (action.type === 'divider') {
        if (cleaned.length === 0 || prevIsDivider) return;
        prevIsDivider = true;
      } else {
        prevIsDivider = false;
      }
      cleaned.push(action);
    });

    if (cleaned[cleaned.length - 1]?.type === 'divider') {
      cleaned.pop();
    }

    return cleaned;
  }

  const moreOptions = computed(() => {
    const filtered = props.options.filter((e) =>
      e.type === 'divider' || e.allPermission ? hasAllPermission(e.permission) : hasAnyPermission(e.permission)
    );
    return cleanDividers(filtered);
  });

  function clickOutSide() {
    emit('close');
  }

  // 初始化
  watch(
    () => props.options,
    (newList) => {
      newList.forEach((item) => {
        if (item.popConfirmProps) {
          popShow.value[item.key as string] = false;
        }
      });
    },
    { immediate: true }
  );
</script>

<style lang="less">
  .crm-dropdown-btn {
    padding: 0 8px;
    min-width: 60px;
    height: 30px;
    @apply flex items-center rounded;
    .n-dropdown-option-body {
      @apply w-full;
    }
  }
  .crm-more-action--size {
    &-small {
      padding: 3px !important;
    }
    &-medium {
      padding: 7px !important;
    }
    &-large {
      padding: 10px !important;
    }
  }
</style>
