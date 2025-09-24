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
        <CrmIcon name="iconicon_file-image_colorful" width="40px" height="40px" />
        <div class="flex flex-1 flex-col gap-[2px]">
          <div class="flex items-center justify-between">
            <div class="one-line-text flex-1">{{ file.name }}</div>
            <div class="flex items-center gap-[16px]">
              <CrmTextButton
                icon="iconicon_delete"
                color="var(--error-red)"
                icon-size="16px"
                @click="handleDelete(file)"
              />
              <a :href="file.url" :download="file.name">
                <CrmTextButton icon="iconicon_download" icon-size="16px" />
              </a>
            </div>
          </div>
          <div class="text-[12px] text-[var(--text-n4)]">{{ file.size }}</div>
          <div class="text-[12px] text-[var(--text-n4)]">{{ file.createText }}</div>
        </div>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import { showConfirmDialog } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  const props = defineProps<{
    fileList: Record<string, any>[];
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  function handleDelete(file: Record<string, any>) {
    showConfirmDialog({
      title: t('crm.fileListPop.deleteTipTitle'),
      message: t('crm.fileListPop.deleteTipContent'),
    })
      .then(() => {
        // on confirm
      })
      .catch(() => {
        // on cancel
      });
  }
</script>

<style lang="less" scoped>
  .crm-file-item {
    @apply flex w-full items-center;

    gap: 16px;
    padding: 8px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
  }
</style>
