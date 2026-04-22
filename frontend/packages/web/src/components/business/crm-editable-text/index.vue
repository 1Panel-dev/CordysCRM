<template>
  <n-input
    v-if="isEditing"
    ref="inputRef"
    v-model:value="inputValue"
    :maxlength="255"
    clearable
    @update-value="handleInput"
    @keydown.enter="confirmEdit"
    @blur="handleBlur"
  />
  <div
    v-else
    class="crm-editable-text-view flex min-w-0 max-w-full items-center gap-[8px]"
    :class="{ 'cursor-pointer': props.clickToEdit }"
    @click="props.clickToEdit ? enableEditMode() : undefined"
  >
    <slot>{{ value }} </slot>
    <CrmIcon
      v-permission="props.permission"
      class="table-row-edit cursor-pointer text-[var(--text-n4)]"
      type="iconicon_edit"
      :size="16"
      @click.stop="enableEditMode"
    />
  </div>
</template>

<script setup lang="ts">
  import { NInput, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const props = defineProps<{
    value: string;
    permission: string[];
    clickToEdit?: boolean;
    emptyTextTip?: string;
  }>();

  const emit = defineEmits<{
    (e: 'handleEdit', value: string, done?: () => void): void;
    (e: 'input', value: string): void;
    (e: 'cancel'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const isEditing = ref(false);
  const inputRef = ref<InstanceType<typeof NInput> | null>(null);
  const inputValue = ref<string>('');

  function enableEditMode() {
    inputValue.value = props.value;
    isEditing.value = true;
    nextTick(() => {
      inputRef.value?.focus();
    });
  }

  function confirmEdit() {
    if (!inputValue.value.trim().length) {
      Message.warning(props.emptyTextTip ?? t('common.value.notNull'));
      return;
    }
    emit('handleEdit', inputValue.value, () => {
      isEditing.value = false;
    });
  }

  function handleBlur() {
    isEditing.value = false;
    emit('cancel');
  }

  function handleInput(value: string) {
    emit('input', value);
  }
</script>

<style lang="less">
  .crm-editable-text-view {
    min-width: 0;
    max-width: 100%;
  }
  .n-data-table {
    .table-row-edit {
      @apply invisible;
    }
    .n-data-table-tr:not(.n-data-table-tr--summary):hover {
      .table-row-edit {
        color: var(--primary-8);
        @apply visible;
      }
    }
  }
</style>
