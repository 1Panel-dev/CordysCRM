<template>
  <n-input
    v-if="isEditing"
    ref="inputRef"
    v-model:value="inputValue"
    class="crm-editable-text-input-wrap"
    :maxlength="255"
    clearable
    @update-value="handleInput"
    @keydown.enter="confirmEdit"
    @blur="handleBlur"
  />
  <div
    v-else
    class="crm-editable-text-view flex min-w-0 max-w-full items-center gap-[8px]"
    :class="{ 'cursor-pointer': props.clickToEdit && hasEditPermission }"
    @click="props.clickToEdit && hasEditPermission ? enableEditMode() : undefined"
  >
    <slot>{{ value }} </slot>
    <CrmIcon
      v-if="hasEditPermission"
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

  import { hasAnyPermission } from '@/utils/permission';

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

  // 在 X6 节点环境里很可能拿不到 n-message-provider，useMessage() 会直接抛错，导致节点组件初始化失败
  const messageApi = (() => {
    try {
      return useMessage();
    } catch (error) {
      return null;
    }
  })();

  const isEditing = ref(false);
  const inputRef = ref<InstanceType<typeof NInput> | null>(null);
  const inputValue = ref<string>('');
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
