<template>
  <CrmPopConfirm
    v-model:show="showPopModal"
    :title="props.title ?? ''"
    icon-type="error"
    :content="content"
    :disabled="!props.title || !props.value || !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']) || props.disabled"
    :positive-text="t('system.message.confirmClose')"
    placement="bottom-end"
    :loading="props.loading"
    @confirm="confirmHandler"
    @cancel="handleCancel"
  >
    <n-tooltip :delay="300" :disabled="!props.toolTipContent">
      <template #trigger>
        <n-switch
          :value="props.value"
          :disabled="props.disabled"
          class="mr-[8px]"
          :rubber-band="false"
          size="small"
          @update:value="changeStatus"
        />
      </template>
      {{ props.toolTipContent }}
    </n-tooltip>
  </CrmPopConfirm>
  {{ t(props.titleColumnText ?? '') }}
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { NSwitch, NTooltip } from 'naive-ui';

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
    disabled: boolean;
    toolTipContent?: string;
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
