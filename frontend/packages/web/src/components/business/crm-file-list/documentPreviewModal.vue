<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :title="props.file?.name || t('common.preview')"
    :mask-closable="false"
    class="crm-document-preview-modal"
    @after-leave="clearViewer"
  >
    <div class="relative h-full w-full bg-[var(--text-n9)]">
      <div ref="viewerElement" class="absolute inset-0 h-full w-full"></div>
      <div
        v-if="loading"
        class="absolute inset-0 flex flex-col items-center justify-center gap-[12px] bg-[var(--text-n9)]"
      >
        <n-spin size="large" />
        <span class="text-[14px] text-[var(--text-n3)]">{{ t('crm.fileListModal.previewLoading') }}</span>
      </div>
      <div
        v-if="errorMessage"
        class="absolute inset-0 flex items-center justify-center bg-[var(--text-n9)] px-[24px] text-center text-[14px] text-[var(--error-red)]"
      >
        {{ errorMessage }}
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
  import { nextTick, ref, watch } from 'vue';
  import { NModal, NSpin } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type { AttachmentInfo } from '@/components/business/crm-form-create/types';

  import { downloadAttachment } from '@/api/modules';

  import type { Viewer } from 'xq-doc-viewer';

  const props = defineProps<{
    file: AttachmentInfo | null;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const { t } = useI18n();
  const viewerElement = ref<HTMLDivElement | null>(null);
  const loading = ref(false);
  const errorMessage = ref('');
  let viewer: Viewer | null = null;
  let requestId = 0;

  async function loadDocument() {
    const { file } = props;
    if (!file) return;

    const currentRequestId = ++requestId;
    loading.value = true;
    errorMessage.value = '';
    try {
      await nextTick();
      const sourceFile = file.file || new File([await downloadAttachment(file.id)], file.name);
      const { createViewer } = await import('xq-doc-viewer');
      if (currentRequestId !== requestId || !viewerElement.value) return;

      viewer = createViewer({
        target: viewerElement.value,
        theme: 'light',
        locale: 'zh-CN',
        toolbar: {
          zoom: true,
          rotate: true,
          pagination: true,
          fullscreen: true,
          print: true,
          download: true,
        },
        onError: (error: Error) => {
          errorMessage.value = error.message || t('crm.fileListModal.previewFailed');
          loading.value = false;
        },
      });
      await viewer.mount();
      if (currentRequestId !== requestId || !viewer) return;
      await viewer.setFile(sourceFile);
    } catch {
      if (currentRequestId === requestId) {
        errorMessage.value = t('crm.fileListModal.previewFailed');
      }
    } finally {
      if (currentRequestId === requestId) {
        loading.value = false;
      }
    }
  }

  function clearViewer() {
    requestId += 1;
    viewer?.destroy();
    viewer = null;
    loading.value = false;
  }

  watch(show, (visible) => {
    if (visible) {
      loadDocument();
    } else {
      clearViewer();
    }
  });
</script>

<style lang="less">
  .crm-document-preview-modal {
    margin-top: 6vh;
    height: 94vh;
  }
  .xq-doc-viewer__branding {
    display: none;
  }
  .docx-wrapper {
    .docx {
      overflow-x: auto;
    }
  }
</style>
