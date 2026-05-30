<template>
  <div class="config-form-card">
    <div class="mb-[8px] flex items-center justify-between">
      <div class="flex items-center">
        {{ props.title }}
        <CrmTag v-show="fileList[0]?.file" theme="light" type="warning" class="ml-[4px]" size="small">
          {{ t('common.unsaved') }}
        </CrmTag>
      </div>
      <CrmUpload
        v-model:file-list="fileList"
        accept="image"
        class="w-[auto]"
        :default-content="false"
        :max-size="props.maxSize"
        :size-unit="props.sizeUnit"
      >
        <n-button v-permission="['SYSTEM_SETTING:UPDATE']" type="default" class="outline--secondary" size="tiny">
          {{ t('system.business.page.replace') }}
        </n-button>
      </CrmUpload>
    </div>
    <p class="text-[12px] text-[var(--text-n4)]">
      {{ props.tip }}
    </p>
  </div>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmUpload from '@/components/pure/crm-upload/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';

  import { PageConfig } from '@/store/modules/app/types';

  export interface UploadCardItem {
    valueKey: keyof PageConfig;
    title: string;
    tip: string;
    showTag?: boolean;
    maxSize: number; // 文件大小限制，单位 MB
    sizeUnit: 'MB' | 'KB'; // 文件大小单位
  }
  const props = defineProps<UploadCardItem>();

  const fileList = defineModel<CrmFileItem[]>('fileList', {
    required: true,
  });

  const { t } = useI18n();
</script>

<style lang="less" scoped>
  .config-form-card {
    margin-bottom: 8px;
    padding: 12px;
    border: 1px solid var(--text-n7);
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
  }
</style>
