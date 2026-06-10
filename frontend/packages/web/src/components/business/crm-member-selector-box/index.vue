<template>
  <div class="w-full">
    <div class="flex items-center justify-between">
      <div class="font-medium">
        {{ label }}
        <span v-if="required" class="text-[var(--error-red)]">*</span>
      </div>
      <n-button
        :disabled="props.disabled || props.clearDisabled || !selectedList?.length"
        text
        type="primary"
        @click="handleClear"
      >
        {{ clearText || t('common.clear') }}
      </n-button>
    </div>

    <div class="crm-member-selector-box__body" :class="{ 'crm-member-selector-box__body--error': isMemberInvalid }">
      <CrmUserTagSelector
        v-model:value="value"
        v-model:selected-list="selectedList"
        :multiple="maxCount !== 1"
        :disabled="props.disabled"
        :drawer-title="label"
        :max-tag-count="false"
        :api-type-key="apiTypeKey"
        :member-types="memberTypes"
        :disabled-node-types="disabledNodeTypes"
        :max-count="maxCount"
        @confirm="emit('confirm')"
        @delete-tag="handleDeleteTag"
      />
      <div v-if="!selectedList?.length" class="crm-member-selector-box__placeholder">
        <CrmIcon type="iconicon_add" :size="16" />
        <span>{{ addText }}</span>
      </div>
    </div>

    <div
      v-if="tipText"
      class="mt-[4px] text-[12px]"
      :class="isMemberInvalid ? 'text-[var(--error-red)]' : 'text-[var(--text-n4)]'"
    >
      {{ tipText }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { NButton } from 'naive-ui';

  import { MemberApiTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  const props = withDefaults(
    defineProps<{
      label?: string;
      addText?: string;
      required?: boolean;
      clearText?: string;
      clearDisabled?: boolean;
      preserveValueOnClear?: boolean;
      maxCount?: number;
      tipText?: string;
      memberTypes?: Option[];
      apiTypeKey?: MemberApiTypeEnum;
      disabledNodeTypes?: DeptNodeTypeEnum[];
      disabled?: boolean;
    }>(),
    {
      label: '',
      addText: '',
      required: false,
      preserveValueOnClear: false,
      maxCount: 1,
    }
  );

  const { t } = useI18n();

  const value = defineModel<string[]>('value', {
    default: () => [],
  });
  const selectedList = defineModel<SelectedUsersItem[]>('selectedList', {
    default: () => [],
  });

  const emit = defineEmits<{
    (e: 'clear'): void;
    (e: 'confirm'): void;
    (e: 'deleteTag'): void;
  }>();

  const isMemberInvalid = computed(() => {
    const selectedCount = selectedList.value.length;
    return (props.required && selectedCount === 0) || Boolean(props.maxCount && selectedCount > props.maxCount);
  });

  function handleDeleteTag() {
    value.value = selectedList.value.map((item) => item.id);
    emit('deleteTag');
  }

  function handleClear() {
    if (props.disabled || props.clearDisabled) {
      return;
    }
    if (!props.preserveValueOnClear) {
      value.value = [];
      selectedList.value = [];
    }
    emit('clear');
  }
</script>

<style scoped lang="less">
  .crm-member-selector-box__body {
    position: relative;
    padding: 4px;
    min-height: 54px;
    border: 1px dashed var(--text-n7);
    border-radius: 3px;
    &.crm-member-selector-box__body--error {
      border-color: var(--error-red);
    }
    :deep(.n-select) {
      min-height: 44px;
    }
    :deep(.n-base-selection__border),
    :deep(.n-base-selection__state-border),
    :deep(.n-base-selection-placeholder) {
      display: none;
    }
    :deep(.n-base-selection-tags) {
      overflow-y: auto;
      padding: 0;
      max-height: 108px;
      align-content: flex-start;
      .crm-scroll-bar();
    }
    :deep(.n-base-selection.n-base-selection--disabled) .n-base-selection-tags {
      background: transparent;
    }
  }
  .crm-member-selector-box__placeholder {
    position: absolute;
    display: flex;
    justify-content: center;
    align-items: center;
    color: var(--primary-8);
    inset: 0;
    gap: 8px;
    pointer-events: none;
  }
</style>
