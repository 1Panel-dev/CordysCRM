<template>
  <div class="w-full">
    <div class="flex items-center justify-between">
      <div class="font-medium">
        {{ label }}
        <span v-if="required" class="text-[var(--error-red)]">*</span>
      </div>
      <n-button :disabled="props.disabled || !selectedList?.length" text type="primary" @click="handleClear">
        {{ t('common.clear') }}
      </n-button>
    </div>

    <div class="approval-member-selector__box" :class="{ 'approval-member-selector__box--error': isMemberInvalid }">
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
        @delete-tag="handleDeleteTag"
      />
      <div v-if="!selectedList?.length" class="approval-member-selector__placeholder">
        <CrmIcon type="iconicon_add" :size="16" />
        <span>{{ addText }}</span>
      </div>
    </div>

    <div
      v-if="maxCount"
      class="mt-[4px] text-[12px]"
      :class="isMemberInvalid ? 'text-[var(--error-red)]' : 'text-[var(--text-n4)]'"
    >
      {{ memberLimitTip }}
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
      maxCount?: number;
      memberTypes?: Option[];
      apiTypeKey?: MemberApiTypeEnum;
      disabledNodeTypes?: DeptNodeTypeEnum[];
      limitLabel?: string;
      disabled?: boolean;
    }>(),
    {
      label: '',
      addText: '',
      required: false,
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

  const isMemberInvalid = computed(() => {
    const selectedCount = selectedList.value.length;
    return (props.required && selectedCount === 0) || Boolean(props.maxCount && selectedCount > props.maxCount);
  });

  const memberLimitTip = computed(() =>
    isMemberInvalid.value
      ? t('process.process.flow.addMemberLimitTip', { count: props.maxCount, target: props.limitLabel })
      : t('process.process.flow.maxAddMemberTip', { count: props.maxCount, target: props.limitLabel })
  );

  function handleDeleteTag() {
    value.value = selectedList.value.map((item) => item.id);
  }

  function handleClear() {
    if (props.disabled) {
      return;
    }
    value.value = [];
    selectedList.value = [];
  }
</script>

<style scoped lang="less">
  .approval-member-selector__box {
    position: relative;
    padding: 4px;
    min-height: 54px;
    border: 1px dashed var(--text-n7);
    border-radius: 3px;
    &.approval-member-selector__box--error {
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
  .approval-member-selector__placeholder {
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
