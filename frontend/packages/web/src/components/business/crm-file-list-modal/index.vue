<template>
  <CrmModal v-model:show="show" :title="t('crm.fileListModal.title')" :footer="false">
    <div class="flex flex-col gap-[8px]">
      <div v-for="file in props.files" :key="file.id" class="crm-file-item">
        <div class="flex flex-1 items-center gap-[12px]">
          <CrmFileIcon :type="file.type" :size="32" />
          <div class="flex flex-1 flex-col gap-[2px]">
            <span class="text-[var(--text-n2)]">{{ file.name }}</span>
            <div class="flex items-center gap-[8px] text-[12px] text-[var(--text-n4)]">
              {{ `${(file.size / 1024).toFixed(2)} KB` }}
              {{
                t('crm.fileListModal.uploadAt', {
                  name: file.createUser,
                  time: dayjs(file.createTime).format('YYYY-MM-DD HH:mm:ss'),
                })
              }}
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
          <template v-if="/(jpg|jpeg|png|gif|bmp|webp|svg)$/i.test(file.type)">
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
  import { NButton, NDivider, NImagePreview, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { isDingTalkBrowser, isWeComBrowser } from '@lib/shared/method';

  import CrmFileIcon from '@/components/pure/crm-file-icon/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';

  import { downloadAttachment } from '@/api/modules';

  import { AttachmentInfo } from '../crm-form-create/types';

  const props = defineProps<{
    files: AttachmentInfo[];
  }>();
  const emit = defineEmits<{
    (e: 'deleteFile', id: string): void;
  }>();

  const Message = useMessage();
  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  async function handleDelete(file: AttachmentInfo, close: () => void) {
    close();
    emit('deleteFile', file.id);
  }

  const showPreview = ref(false);
  const previewSrc = ref('');
  function handlePreview(file: AttachmentInfo) {
    previewSrc.value = `${PreviewAttachmentUrl}/${file.id}`;
    showPreview.value = true;
  }

  async function handleDownload(file: AttachmentInfo) {
    if (isWeComBrowser() || isDingTalkBrowser()) {
      Message.warning(t('crm.fileListModal.wxworkDownloadTip'));
      return;
    }
    try {
      const res = await downloadAttachment(file.id);
      const url = URL.createObjectURL(new Blob([res], { type: 'application/octet-stream' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = file.name;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
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
