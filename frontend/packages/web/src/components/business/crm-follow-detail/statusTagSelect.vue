<template>
  <n-select
    v-model:value="status"
    :placeholder="t('common.pleaseSelect')"
    :render-tag="renderTag"
    :options="options"
    :show-checkmark="false"
    :render-option="renderOption"
    :disabled="props.disabled"
    :bordered="false"
    class="follow-plan-status-select"
    @update:value="updateValue"
  />
</template>

<script setup lang="ts">
  import { VNodeChild } from 'vue';
  import { NSelect, SelectOption } from 'naive-ui';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { StatusTagKey } from '@lib/shared/models/customer';

  import StatusTag from './statusTag.vue';

  import { statusMap } from '@/config/follow';

  const { t } = useI18n();

  const props = defineProps<{
    disabled: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'change'): void;
  }>();

  const status = defineModel<CustomerFollowPlanStatusEnum>('status', {
    required: true,
  });

  const options = computed(() =>
    Object.values(statusMap).map((e) => ({
      ...e,
      label: t(e.label),
    }))
  );

  const renderTag = ({ option }: { option: SelectOption; handleClose: () => void }) => {
    return h(StatusTag, {
      class: `${props.disabled ? '' : 'cursor-pointer'}`,
      status: option.value as StatusTagKey,
      hiddenDownIcon: props.disabled,
    });
  };

  function renderOption({ node, option }: { node: VNode; option: SelectOption }): VNodeChild {
    node.children = [
      h(StatusTag, {
        status: option.value as StatusTagKey,
        hiddenDownIcon: true,
      }),
    ];
    return node;
  }

  function updateValue() {
    emit('change');
  }
</script>

<style lang="less">
  .follow-plan-status-select {
    width: auto;
    .n-base-selection {
      border: 1px solid transparent;
      &.n-base-selection--disabled {
        .n-base-selection-label {
          background-color: transparent !important;
        }
      }
      .n-base-selection-input {
        padding: 0;
      }
      .n-base-suffix {
        display: none !important;
        .n-base-suffix__arrow {
          display: none !important;
        }
      }
    }
    .n-base-selection-input {
      padding: 0;
    }
  }
</style>
