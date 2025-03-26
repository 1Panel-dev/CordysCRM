<template>
  <CrmPopConfirm
    v-model:show="showPopModal"
    :title="props.title ?? ''"
    icon-type="error"
    :content="content"
    :disabled="!props.title || innerValue"
    :positive-text="t('system.message.confirmClose')"
    placement="bottom-end"
    :loading="props.loading"
    @confirm="confirmHandler"
  >
    <n-switch
      v-model:value="innerValue"
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

  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';

  import { useI18n } from '@/hooks/useI18n';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();

  const props = defineProps<{
    titleColumnText?: string;
    title?: string;
    loading: boolean;
    content?: string;
  }>();

  const emit = defineEmits<{
    (e: 'change', val: boolean, cancel?: () => void): void;
  }>();

  const innerValue = defineModel<boolean>('value', {
    required: false,
    default: false,
  });

  const showPopModal = ref(false);

  function handleCancel() {
    showPopModal.value = false;
  }

  function changeStatus() {
    if (innerValue.value) {
      emit('change', innerValue.value, handleCancel);
    } else {
      showPopModal.value = true;
    }
  }

  function confirmHandler() {
    emit('change', innerValue.value, handleCancel);
  }
</script>
