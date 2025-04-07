<template>
  <CrmPopConfirm
    v-model:show="showPopModal"
    :title="props.title ?? ''"
    icon-type="error"
    :content="content"
    :disabled="!props.title || !props.value"
    :positive-text="t('system.message.confirmClose')"
    placement="bottom-end"
    :loading="props.loading"
    @confirm="confirmHandler"
    @cancel="handleCancel"
  >
    <n-switch
      :value="props.value"
      :disabled="!hasAnyPermission(['SYSTEM_NOTICE:UPDATE'])"
      class="mr-[8px]"
      size="small"
      @click="changeStatus"
    />
  </CrmPopConfirm>
  {{ t(props.titleColumnText ?? '') }}
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { NSwitch } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';

  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();

  const props = defineProps<{
    titleColumnText?: string;
    title?: string;
    loading: boolean;
    content?: string;
    value: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'change', cancel?: () => void): void;
  }>();

  const showPopModal = ref(false);

  function handleCancel() {
    showPopModal.value = false;
  }

  function changeStatus() {
    if (!hasAnyPermission(['SYSTEM_NOTICE:UPDATE'])) return;

    if (!props.title || !props.value) {
      emit('change', handleCancel);
      return;
    }
    showPopModal.value = true;
  }

  function confirmHandler() {
    emit('change', handleCancel);
  }
</script>
