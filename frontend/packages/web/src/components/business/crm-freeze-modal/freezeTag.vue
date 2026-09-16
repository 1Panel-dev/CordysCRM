<template>
  <n-popover class="!p-[16px]" trigger="hover" placement="bottom" :show-arrow="false">
    <template #trigger>
      <CrmTag theme="lightOutLine" type="warning">{{ t('common.frozen') }}</CrmTag>
    </template>
    <div class="flex w-[300px] flex-col gap-[8px]">
      <div class="mr-auto flex items-center gap-[8px]">
        <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
        <div class="font-semibold">
          {{ `${props.resourceType === 'customer' ? t('menu.customer') : t('menu.lead')}${t('common.frozen')}` }}
        </div>
      </div>
      <div class="freeze-reason">{{ props.freezeReason }}</div>
      <div class="flex items-center justify-between text-[12px]">
        <div class="text-[var(--text-n4)]">
          {{ t('common.unfreezeTime') }}
        </div>
        {{
          props.freezeType === 'freezeForever'
            ? t('common.freezeForever')
            : dayjs(props.unfreezeTime).format('YYYY-MM-DD HH:mm:ss')
        }}
      </div>
    </div>
  </n-popover>
</template>

<script setup lang="ts">
  import { NPopover } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  const props = defineProps<{
    resourceType: 'customer' | 'lead';
    freezeType: 'custom' | 'freezeForever';
    freezeReason: string;
    unfreezeTime: number;
  }>();

  const { t } = useI18n();
</script>

<style lang="less" scoped>
  .freeze-reason {
    // 超出两行文本省略
    display: box;
    overflow: hidden;
    padding: 8px;
    width: 100%;
    background: var(--text-n9);
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
</style>
