<template>
  <n-input
    v-if="isEditing"
    ref="inputRef"
    v-model:value="inputValue"
    class="crm-editable-text-input-wrap"
    :maxlength="255"
    :placeholder="props.placeholder ?? t('common.pleaseInput')"
    clearable
    @update-value="handleInput"
    @keydown.enter.prevent="handleEnterConfirm"
    @blur="handleBlurConfirm"
  />
  <div
    v-else
    class="crm-editable-text-view flex min-w-0 max-w-full items-center gap-[8px]"
    :class="{ 'cursor-pointer': props.clickToEdit && hasEditPermission }"
    @click="props.clickToEdit && hasEditPermission && !disabled ? enableEditMode() : undefined"
  >
    <slot>{{ value }} </slot>
    <CrmIcon
      v-if="hasEditPermission && !disabled"
      class="table-row-edit cursor-pointer text-[var(--text-n4)]"
      type="iconicon_edit"
      :size="16"
      @click.stop="enableEditMode"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { NInput, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import useDiscreteApi from '@/hooks/useDiscreteApi';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    value: string;
    permission: string[];
    clickToEdit?: boolean;
    emptyTextTip?: string;
    placeholder?: string;
    disabled?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'handleEdit', value: string, done?: () => void): void;
    (e: 'input', value: string): void;
  }>();

  const { t } = useI18n();

  // 在 X6 节点环境里很可能拿不到 n-message-provider，useMessage() 会直接抛错，导致节点组件初始化失败
  const messageApi = (() => {
    try {
      return useMessage();
    } catch (error) {
      return useDiscreteApi().message;
    }
  })();

  const isEditing = ref(false);
  const inputRef = ref<InstanceType<typeof NInput> | null>(null);
  const inputValue = ref<string>('');
  const skipBlurConfirm = ref(false);
  const hasEditPermission = computed(() => hasAnyPermission(props.permission));

  function enableEditMode() {
    inputValue.value = props.value;
    isEditing.value = true;
    nextTick(() => {
      inputRef.value?.focus();
    });
  }

  function confirmEdit() {
    if (!inputValue.value.trim().length) {
      const message = props.emptyTextTip ?? t('common.value.notNull');
      if (messageApi) {
        messageApi.warning(message);
      } else {
        // eslint-disable-next-line no-console
        console.warn(message);
      }
      return;
    }
    emit('handleEdit', inputValue.value, () => {
      isEditing.value = false;
    });
  }

  function handleEnterConfirm() {
    skipBlurConfirm.value = true;
    confirmEdit();
  }

  function handleBlurConfirm() {
    if (skipBlurConfirm.value) {
      skipBlurConfirm.value = false;
      return;
    }

    confirmEdit();
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
