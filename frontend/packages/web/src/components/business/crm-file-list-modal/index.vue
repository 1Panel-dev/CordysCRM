<template>
  <CrmModal v-model:show="show" :title="t('crm.fileListModal.title')" :footer="false">
    <div class="flex flex-col gap-[8px]">
      <div v-for="file in props.files" :key="file.id" class="crm-file-item">
        <div class="flex flex-1 items-center gap-[12px]">
          <CrmIon type="iconicon_file-image_colorful" :size="32" />
          <div class="flex flex-1 flex-col gap-[2px]">
            <span class="text-[var(--text-n2)]">{{ file.name }}</span>
            <div class="flex items-center gap-[8px] text-[12px] text-[var(--text-n4)]">
              {{ file.size }}
              {{ file.createText }}
            </div>
          </div>
        </div>
        <div class="flex items-center gap-[4px]">
          <CrmPopConfirm
            :title="t('crm.fileListModal.deleteTipTitle')"
            icon-type="error"
            :content="t('crm.fileListModal.deleteTipContent')"
            :positive-text="t('common.confirm')"
            trigger="click"
            :negative-text="t('common.cancel')"
            placement="bottom-end"
            @confirm="handleDelete(file, $event)"
          >
            <n-button type="error" text>{{ t('common.delete') }}</n-button>
          </CrmPopConfirm>
          <template v-if="file.type.includes('image')">
            <n-divider vertical />
            <n-button type="default" text @click="handlePreview(file)">{{ t('common.preview') }}</n-button>
          </template>
          <n-divider vertical />
          <n-button type="default" text @click="handleDownload(file)">{{ t('common.download') }}</n-button>
        </div>
      </div>
    </div>
    <n-image-preview v-model:show="showPreview" :src="previewSrc" />
  </CrmModal>
</template>

<script setup lang="ts">
  import { NButton, NDivider, NImagePreview } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';

  const props = defineProps<{
    files: Record<string, any>[];
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  function handleDelete(file: Record<string, any>, close: () => void) {
    // Handle delete file
    close();
  }

  const showPreview = ref(false);
  const previewSrc = ref('');
  function handlePreview(file: Record<string, any>) {
    previewSrc.value = file.url;
    showPreview.value = true;
  }

  function handleDownload(file: Record<string, any>) {
    window.open(file.url, '_blank');
  }
</script>

<style lang="less">
  .crm-file-item {
    @apply flex w-full items-center justify-between;

    padding: 8px 12px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
  }
</style>
