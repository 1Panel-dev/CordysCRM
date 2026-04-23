<template>
  <CrmModal v-model:show="show" :title="t('crm.fileListModal.title')" :footer="false">
    <CrmFileList :files="props.files" :readonly="props.readonly" @deleteFile="emit('deleteFile', $event)" />
  </CrmModal>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmFileList from '../crm-file-list/index.vue';

  import { AttachmentInfo } from '../crm-form-create/types';

  const props = defineProps<{
    files: AttachmentInfo[];
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'deleteFile', id: string): void;
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });
</script>
