<template>
  <n-input
    v-if="isEditing"
    ref="inputRef"
    v-model:value="inputValue"
    :max-length="255"
    clearable
    @keydown.enter="confirmEdit"
    @blur="handleBlur"
  />
  <div v-else class="flex items-center gap-[8px]">
    <slot>{{ value }} </slot>
    <CrmIcon class="cursor-pointer text-[var(--text-n4)]" type="iconicon_edit" :size="16" @click="enableEditMode" />
  </div>
</template>

<script setup lang="ts">
  import { NInput, useMessage } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  const props = defineProps<{
    value: string;
  }>();

  const emit = defineEmits<{
    (e: 'handleEdit', value: string): void;
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
      Message.warning(t('common.value.notNull'));
      return;
    }
    emit('handleEdit', inputValue.value);
    isEditing.value = false;
  }

  function handleBlur() {
    isEditing.value = false;
  }
</script>
