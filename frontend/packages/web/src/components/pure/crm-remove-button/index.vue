<template>
  <CrmPopConfirm
    v-model:show="showModal"
    v-permission="props.permission"
    :title="props.title"
    :content="props.content"
    :loading="props.loading"
    :icon-type="props.iconType"
    :placement="props.placement || 'bottom-end'"
    @confirm="handleConfirm"
    @cancel="handleCancel"
    @update:show="updateShow"
  >
    <slot>
      <n-button quaternary type="primary" class="text-btn-primary" @click="handleRemove">
        {{ t('common.remove') }}
      </n-button>
    </slot>
  </CrmPopConfirm>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';

  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    loading: boolean;
    title: string; // 标题
    content: string; // 内容
    iconType?: 'error' | 'warning' | 'primary'; // 图标类型
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
    permission?: string[]; // 权限
  }>();

  const emit = defineEmits<{
    (e: 'confirm', cancel: () => void): void;
  }>();

  const showModal = ref<boolean>(false);

  function handleCancel() {
    showModal.value = false;
  }

  function handleConfirm() {
    emit('confirm', handleCancel);
  }

  function handleRemove() {
    showModal.value = true;
  }

  function updateShow(val: boolean) {
    showModal.value = val;
  }
</script>

<style scoped></style>
