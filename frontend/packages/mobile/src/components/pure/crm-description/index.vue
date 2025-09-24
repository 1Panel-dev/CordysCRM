<template>
  <van-cell-group inset class="h-full">
    <div class="crm-description flex h-full flex-col gap-[8px] overflow-auto px-[20px] py-[16px]">
      <div v-for="item of props.description" :key="item.label" class="crm-description-item">
        <slot :name="item.slotName" :item="item">
          <div v-if="item.isTitle" class="crm-description-title">{{ item.label }}</div>
          <template v-else>
            <div class="crm-description-label">{{ item.label }}</div>
            <div class="crm-description-value">
              <slot :name="item.valueSlotName" :item="item">
                <template v-if="item.isImage">
                  <div v-if="(item.value as string[])?.length === 0">-</div>
                  <template v-else>
                    <van-image
                      v-for="img of item.value"
                      :key="img as string"
                      width="40"
                      height="40"
                      :src="`${PreviewPictureUrl}/${img}`"
                      @click="
                  () => {
                    showImagePreview({
                      images: (item.value as string[]).map((img) => `${PreviewPictureUrl}/${img}`),
                      closeable: true,
                      startPosition: (item.value as string[]).findIndex((image) => image === img),
                    });
                  }
                "
                    />
                  </template>
                </template>
                <div v-else-if="item.isAttachment" class="text-[var(--primary-8)]" @click="handleAttachmentClick(item)">
                  {{ t('crm.description.attachment', { count: (item.value as any[])?.length }) }}
                </div>
                <CrmTag v-else-if="Array.isArray(item.value) && item.value?.length" :tag="item.value as any || ''" />
                <div v-else>{{ getDisplayValue(item.value as string) }}</div>
              </slot>
            </div>
          </template>
        </slot>
      </div>
    </div>
  </van-cell-group>
  <CrmFileListPop v-model:show="showFileListPop" :file-list="activeFileList" />
</template>

<script setup lang="ts">
  import { showImagePreview } from 'vant';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmFileListPop from '@/components/business/crm-file-list-pop/index.vue';

  export interface CrmDescriptionItem {
    label: string;
    value?: string | number | (string | number)[] | Record<string, any>[];
    isTag?: boolean;
    isImage?: boolean;
    isTitle?: boolean;
    slotName?: string;
    valueSlotName?: string;
    isAttachment?: boolean;
    [key: string]: any;
  }

  const { t } = useI18n();

  const props = defineProps<{
    description: CrmDescriptionItem[];
  }>();

  const getDisplayValue = (val?: string | number | (string | number)[]) => {
    if (typeof val === 'number') return val;
    if (typeof val === 'string' || Array.isArray(val)) return val.length > 0 ? val : '-';
    return '-';
  };

  const showFileListPop = ref(true);
  const activeFileList = ref<Record<string, any>[]>([]);
  function handleAttachmentClick(item: CrmDescriptionItem) {
    activeFileList.value = item.value as Record<string, any>[];
    showFileListPop.value = true;
  }
</script>

<style lang="less" scoped>
  .crm-description-item {
    @apply flex;

    gap: 16px;
    .crm-description-title {
      margin: 4px 0;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-n1);
    }
    .crm-description-label {
      @apply break-words;

      width: 20%;
      font-size: 14px;
      color: var(--text-n2);
    }
    .crm-description-value {
      @apply flex-1;

      font-size: 14px;
      text-align: justify;
      color: var(--text-n1);
    }
  }
</style>
