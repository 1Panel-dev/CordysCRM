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
    <fileList :file-list="props.fileList" />
  </van-popup>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';
  import fileList from './fileList.vue';
  import { AttachmentInfo } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fileList: AttachmentInfo[];
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });
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
