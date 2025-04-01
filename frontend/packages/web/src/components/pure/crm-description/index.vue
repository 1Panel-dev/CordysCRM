<template>
  <n-scrollbar>
    <div class="crm-description">
      <div
        v-for="(item, index) of props.descriptions"
        :key="item.label"
        class="crm-description-item"
        :class="item.class"
        :style="{ marginBottom: props.descriptions.length - index <= props.column ? '' : `${props.lineGap}px` }"
      >
        <slot :name="item.slotName" :item="item">
          <div
            class="crm-description-item-label"
            :style="{ width: props.labelWidth || '120px', textAlign: props.labelAlign }"
          >
            <n-tooltip>
              <template #trigger>
                <div :class="props.oneLineLabel ? 'one-line-text' : ''">
                  {{ item.label }}
                </div>
              </template>
              {{ item.label }}
            </n-tooltip>
          </div>
          <div :class="getValueClass()" :style="{ textAlign: props.valueAlign }">
            <slot :name="item.valueSlotName" :item="item">
              <CrmTagGroup
                v-if="Array.isArray(item.value)"
                :tags="item.value"
                :label-key="item.tagProps?.labelKey"
                :class="`justify-${props.valueAlign}`"
              />
              <n-tooltip
                v-else
                :disabled="item.value === undefined || item.value === null || item.value?.toString() === ''"
                :placement="item.tooltipPosition ?? 'top-start'"
              >
                <template #trigger>
                  <div>
                    {{
                      item.value === undefined || item.value === null || item.value?.toString() === ''
                        ? '-'
                        : item.value
                    }}
                  </div>
                </template>
                {{ item.value }}
              </n-tooltip>
            </slot>
          </div>
        </slot>
      </div>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar, NTooltip } from 'naive-ui';

  import CrmTagGroup from '@/components/pure/crm-tag-group/index.vue';

  export interface Description {
    label: string;
    key?: string;
    slotName?: string;
    valueSlotName?: string;
    value: string | string[];
    class?: string;
    tooltipPosition?:
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
      | 'left-end'
      | undefined; // 提示位置防止窗口抖动
    tagProps?: {
      labelKey?: string;
    };
    [key: string]: any;
  }

  const props = withDefaults(
    defineProps<{
      column?: number;
      descriptions: Description[];
      labelWidth?: string;
      lineGap?: number;
      labelAlign?: 'center' | 'start' | 'end'; // label 对齐方式
      valueAlign?: 'center' | 'start' | 'end'; // value 对齐方式
      oneLineValue?: boolean; // value 是否单行显示
      oneLineLabel?: boolean; // label 是否单行显示
    }>(),
    {
      column: 1,
      lineGap: 16,
      labelAlign: 'start',
      valueAlign: 'start',
      oneLineLabel: true,
    }
  );

  function getValueClass() {
    if (props.oneLineValue) {
      return 'crm-description-item-value crm-description-item-value--one-line';
    }
    return 'crm-description-item-value';
  }
</script>

<style lang="less" scoped>
  .crm-description {
    @apply flex flex-wrap;
    .crm-description-item {
      @apply flex;

      width: calc(100% / v-bind(column));
    }
    .crm-description-item-label {
      @apply font-normal;

      padding-right: 16px;
      color: var(--text-n2);
      word-wrap: break-word;
    }
    .crm-description-item-value {
      @apply relative flex-1 overflow-hidden break-all align-top;

      text-overflow: ellipsis;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
    }
    .crm-description-item-value--one-line {
      -webkit-line-clamp: 1;
    }
  }
</style>
