<template>
  <van-popup v-model:show="show" destroy-on-close round position="bottom">
    <div class="relative p-[16px] text-center">
      <div class="text-[16px] font-semibold">{{ t('crm.fileListPop.title') }}</div>
      <CrmTextButton
        icon="iconicon_close"
        icon-size="24px"
        color="var(--text-n1)"
        class="absolute right-[16px] top-[16px]"
        @click="show = false"
      />
    </div>
    <div class="flex flex-col gap-[8px] px-[16px] pb-[16px]">
      <div v-for="file in props.fileList" :key="file.id" class="crm-file-item">
        <CrmFileIcon :type="file.type" width="40px" height="40px" @click="handlePreview(file)" />
        <div class="flex flex-1 flex-col gap-[2px] overflow-hidden">
          <div class="flex items-center justify-between">
            <div class="one-line-text flex-1">{{ file.name }}</div>
            <div class="flex items-center gap-[16px]">
              <!-- <CrmTextButton
                icon="iconicon_delete"
                color="var(--error-red)"
                icon-size="16px"
                @click="handleDelete(file)"
              /> -->
              <CrmTextButton icon="iconicon_download" icon-size="16px" @click="handleDownloadAttachment(file)" />
            </div>
          </div>
          <div class="text-[12px] text-[var(--text-n4)]">
            {{ `${(file.size / 1024).toFixed(2)} KB` }}
          </div>
          <div class="text-[12px] text-[var(--text-n4)]">
            {{
              t('crm.fileListPop.uploadAt', {
                name: file.createUser,
                time: dayjs(file.createTime).format('YYYY-MM-DD HH:mm:ss'),
              })
            }}
          </div>
        </div>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import { showImagePreview, showToast } from 'vant';
  import dayjs from 'dayjs';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import {isDingTalBrowserk, isWeComBrowser} from '@lib/shared/method';

  import CrmFileIcon from '@/components/pure/crm-file-icon/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { downloadAttachment } from '@/api/modules';

  import { AttachmentInfo } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fileList: AttachmentInfo[];
  }>();
  // const emit = defineEmits<{
  //   (e: 'deleteFile', id: string): void;
  // }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  // function handleDelete(file: AttachmentInfo) {
  //   showConfirmDialog({
  //     title: t('crm.fileListPop.deleteTipTitle'),
  //     message: t('crm.fileListPop.deleteTipContent'),
  //   }).then(async () => {
  //     emit('deleteFile', file.id);
  //   });
  // }

  async function handleDownloadAttachment(file: AttachmentInfo) {
    if (isWeComBrowser() || isDingTalBrowserk()) {
      showToast(t('crm.fileListPop.wxworkDownloadTip'));
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

  function handlePreview(file: AttachmentInfo) {
    if (/(jpg|jpeg|png|gif|bmp|webp|svg)$/i.test(file.type)) {
      showImagePreview({
        images: [`${PreviewAttachmentUrl}/${file.id}`],
        closeable: true,
      });
    }
  }
</script>

<style lang="less" scoped>
  .crm-file-item {
    @apply flex w-full items-center overflow-hidden;

    gap: 16px;
    padding: 8px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
  }
</style>
