<template>
  <n-popover
    placement="right"
    :show-arrow="false"
    :theme-overrides="{
      padding: '16px',
      color: 'var(--text-n10)',
      textColor: 'var(--text-n1)',
    }"
  >
    <template #trigger>
      <span class="cursor-pointer font-normal text-[var(--primary-8)]">
        {{ t('process.process.flow.viewExample') }}
      </span>
    </template>

    <div class="min-w-[270px]">
      <div
        v-if="tip"
        class="mb-[16px] rounded-[4px] bg-[var(--warning-5)] px-[12px] py-[5px] text-[var(--warning-yellow)]"
      >
        {{ tip }}
      </div>
      <div
        v-for="(item, index) in items"
        :key="`${item.level}-${item.name}`"
        class="grid grid-cols-[86px_minmax(150px,1fr)] items-baseline"
      >
        <div class="text-[var(--text-n1)]">{{ item.level }}</div>
        <div class="flex flex-col items-center">
          <div class="w-full rounded-[4px] bg-[var(--primary-7)] py-[8px] text-center text-[var(--primary-0)]">
            {{ item.name }}
          </div>
          <CrmIcon
            v-if="index < items.length - 1"
            type="iconicon_arrow_up"
            :size="16"
            class="mb-[8px] mt-[2px] text-[var(--text-n4)]"
          />
        </div>
      </div>
    </div>
  </n-popover>
</template>

<script setup lang="ts">
  import { NPopover } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  defineOptions({
    name: 'ApprovalLevelExamplePopover',
  });

  export type ApprovalLevelExampleItem = {
    level: string;
    name: string;
  };

  defineProps<{
    items: ApprovalLevelExampleItem[];
    tip?: string;
  }>();

  const { t } = useI18n();
</script>
