<template>
  <van-cell-group inset class="h-full">
    <div class="flex h-full flex-col gap-[8px] overflow-auto px-[20px] py-[16px]">
      <div v-for="item of props.description" :key="item.label" class="crm-description-item">
        <slot :name="item.slotName" :item="item">
          <div v-if="item.isTitle" class="crm-description-title">{{ item.label }}</div>
          <template v-else>
            <div class="crm-description-label">{{ item.label }}</div>
            <div class="crm-description-value">
              <slot :name="item.valueSlotName" :item="item">
                <template v-if="item.isImage">
                  <div v-if="(item.value as string[])?.length === 0">-</div>
                  <van-image
                    v-for="img of item.value"
                    v-else
                    :key="img"
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
                <CrmTag v-else-if="Array.isArray(item.value) && item.value?.length" :tag="item.value || ''" />
                <div v-else>{{ item.value || '-' }}</div>
              </slot>
            </div>
          </template>
        </slot>
      </div>
    </div>
  </van-cell-group>
</template>

<script setup lang="ts">
  import { showImagePreview } from 'vant';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  export interface CrmDescriptionItem {
    label: string;
    value?: string | number | (string | number)[];
    isTag?: boolean;
    isImage?: boolean;
    isTitle?: boolean;
    slotName?: string;
    valueSlotName?: string;
    [key: string]: any;
  }

  const props = defineProps<{
    description: CrmDescriptionItem[];
  }>();
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
      @apply flex-1 break-words break-all;

      font-size: 14px;
      color: var(--text-n1);
    }
  }
</style>
