<template>
  <van-cell-group inset class="flex flex-col gap-[8px] px-[20px] py-[16px]">
    <div v-for="item of props.description" :key="item.label" class="crm-description-item">
      <slot :name="item.slotName" :item="item">
        <div v-if="item.isTitle" class="crm-description-title">{{ item.label }}</div>
        <template v-else>
          <div class="crm-description-label">{{ item.label }}</div>
          <div class="crm-description-value">
            <slot :name="item.valueSlotName" :item="item">
              <CrmTag v-if="item.isTag && item.value" :tag="item.value || ''" />
              <div v-else>{{ item.value || '-' }}</div>
            </slot>
          </div>
        </template>
      </slot>
    </div>
  </van-cell-group>
</template>

<script setup lang="ts">
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  export interface CrmDescriptionItem {
    label: string;
    value?: string | number | (string | number)[];
    isTag?: boolean;
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
    @apply flex items-center;

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
      color: var(--text-n1);
    }
  }
</style>
